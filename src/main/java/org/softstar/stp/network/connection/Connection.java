package org.softstar.stp.network.connection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.softstar.stp.exception.CorruptedPacketException;
import org.softstar.stp.network.decoder.AbstractPacketDecoder;
import org.softstar.stp.network.decoder.CRC32PacketDecoder;
import org.softstar.stp.network.encoder.AbstractPacketEncoder;
import org.softstar.stp.network.encoder.CRC32PacketEncoder;
import org.softstar.stp.network.packet.*;
import org.softstar.stp.utils.CircularArray;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Connection implements AutoCloseable {
    protected static final int INITIAL_WINDOW_SIZE = 16;
    protected static final int MIN_WINDOW_SIZE = 4;
    protected static final int MAX_WINDOW_SIZE = 128;
    protected static final int SEND_QUEUE_SIZE = 1024;

    protected static final long TICK_LENGTH = 25;
    protected static final long RESEND_TICK = 30;
    protected static final int RESEND_LIMIT = 50;
    protected static final long TIMEOUT_TICK = RESEND_TICK * RESEND_LIMIT * 2;
    protected static final long IDLE_WAIT_TICK = RESEND_TICK * 10;

    protected static final int DATA_PACKET_SIZE = 16384;
    protected static final int PIPE_SIZE = DATA_PACKET_SIZE * MAX_WINDOW_SIZE * 2;

    protected final PipedInputStream dataSendInputStream = new PipedInputStream(PIPE_SIZE);
    protected final PipedOutputStream dataSendOutputStream;
    protected final PipedOutputStream dataReceiveOutputStream = new PipedOutputStream();
    protected final PipedInputStream dataReceiveInputStream;
    protected final BlockingDeque<Packet> sendQueue = new LinkedBlockingDeque<>(SEND_QUEUE_SIZE);
    protected final CircularArray<DataPacket> sendWindow = new CircularArray<>(MAX_WINDOW_SIZE);
    protected final CircularArray<DataPacket> receiveWindow = new CircularArray<>(MAX_WINDOW_SIZE, MAX_WINDOW_SIZE);
    protected final AbstractPacketDecoder packetDecoder;
    protected final AbstractPacketEncoder packetEncoder;
    protected final DatagramChannel channel;
    protected final SocketAddress peerAddress;
    protected volatile ConnectionState state;
    protected long tick = 0;
    protected int windowSize = INITIAL_WINDOW_SIZE;
    protected long nextSeqNumber;
    protected long ackedNum = 0;
    protected long tickMark = 0;
    protected int waitRecord = 0;
    protected long timeoutMark = 0;
    protected volatile boolean finalized = false;
    protected boolean otherFinalized = false;
    protected volatile Exception deadReason = null;

    {
        try {
            dataSendOutputStream = new PipedOutputStream(dataSendInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            dataReceiveInputStream = new PipedInputStream(dataReceiveOutputStream, PIPE_SIZE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection(DatagramChannel channel, SocketAddress peerAddress, ConnectionState initialState, AbstractPacketEncoder encoder, AbstractPacketDecoder decoder) throws IOException {
        this.packetEncoder = encoder;
        this.packetDecoder = decoder;
        this.nextSeqNumber = ThreadLocalRandom.current().nextLong(1, Integer.MAX_VALUE);
        this.state = initialState;
        this.channel = channel;
        this.peerAddress = peerAddress;
        channel.configureBlocking(false);
        new Thread(this::sendLoop).start();
        new Thread(this::loop).start();
    }

    public Connection(DatagramChannel channel, SocketAddress peerAddress, ConnectionState initialState) throws IOException {
        this(channel, peerAddress, initialState, new CRC32PacketEncoder(), new CRC32PacketDecoder());
    }

    public Connection(DatagramChannel channel, SocketAddress peerAddress, boolean isServer) throws IOException {
        this(channel, peerAddress, isServer ? ConnectionState.WAIT_SYN : ConnectionState.TO_SEND_SYN);
    }

    public Connection(DatagramChannel channel, SocketAddress peerAddress, boolean isServer, AbstractPacketEncoder encoder, AbstractPacketDecoder decoder) throws IOException {
        this(channel, peerAddress, isServer ? ConnectionState.WAIT_SYN : ConnectionState.TO_SEND_SYN, encoder, decoder);
    }

    @Nullable
    protected Packet tryRecvPacket() {
        ByteBuffer buffer = ByteBuffer.allocate(65535);
        SocketAddress address;
        try {
            address = channel.receive(buffer);
        } catch (IOException e) {
            deadReason = e;
            state = ConnectionState.DEAD;
            return null;
        }

        if (address == null || !address.equals(peerAddress)) return null;
        buffer.limit(buffer.position());
        buffer.rewind();

        Packet packet;
        try {
            packet = packetDecoder.fromByteBuffer(buffer);
        } catch (CorruptedPacketException e) {
            return null;
        } catch (Exception e) {
            deadReason = e;
            state = ConnectionState.DEAD;
            return null;
        }

        return packet;
    }

    protected void sendPacket(@NotNull Packet packet) {
        sendQueue.offer(packet);
    }

    protected void onRecvPacket(@NotNull Packet packet) {
        timeoutMark = tick;
        switch (state) {
            case TO_SEND_SYN -> {
            }
            case WAIT_SYN_ACK -> {
                if (packet instanceof SynAckPacket) {
                    ackedNum = packet.getSeqNumber();
                    cleanState();
                    state = ConnectionState.READY;
                }
            }
            case WAIT_SYN -> {
                if (packet instanceof SynPacket) {
                    sendPacket(new SynAckPacket(nextSeqNumber));
                    ackedNum = packet.getSeqNumber();
                    cleanState();
                    state = ConnectionState.ACKED_SYN;
                }
            }
            case ACKED_SYN -> {
                if (packet instanceof SynPacket) {
                    sendPacket(new SynAckPacket(nextSeqNumber));
                    tickMark = tick;
                } else {
                    cleanState();
                    state = ConnectionState.READY;
                    onRecvPacket(packet);
                }
            }
            case READY, TO_SEND_FIN, WAIT_FIN_ACK, WAIT_OTHER_FIN -> {
                switch (packet) {
                    case DataPacket data -> {
                        var index = data.getSeqNumber() - ackedNum;
                        if (index < 0 || index >= MAX_WINDOW_SIZE) break;
                        receiveWindow.set((int) index, data);

                        while (receiveWindow.getFirst() != null) {
                            var recv = receiveWindow.removeFirst();
                            receiveWindow.add(null);
                            try {
                                dataReceiveOutputStream.write(recv.getData());
                            } catch (IOException ignored) {
                            }
                            ++ackedNum;
                        }
                    }
                    case AckPacket ack -> {
                        var acked = ack.getAckNumber();
                        boolean flag = false;
                        while (!sendWindow.isEmpty()) {
                            var fst = sendWindow.getFirst();
                            if (fst.getSeqNumber() >= acked) break;
                            flag = true;
                            sendWindow.removeFirst();
                        }
                        if (state == ConnectionState.READY && flag) tickMark = tick;
                    }
                    case FinPacket _ -> {
                        finalized = true;
                        otherFinalized = true;
                        sendPacket(new FinAckPacket(nextSeqNumber));
                    }
                    case FinAckPacket _ -> {
                        if (state != ConnectionState.WAIT_FIN_ACK) break;
                        cleanState();
                        state = ConnectionState.WAIT_OTHER_FIN;
                    }
                    default -> {
                    }
                }
            }
            case LAST_WAIT -> {
                if (packet instanceof FinPacket) {
                    tickMark = tick;
                    sendPacket(new FinAckPacket(nextSeqNumber));
                }
            }
        }
    }

    protected void cleanState() {
        tickMark = tick;
        waitRecord = 0;
    }

    protected void onTick() {
        if (tick - timeoutMark > TIMEOUT_TICK) {
            deadReason = new IOException("Connection timed out");
            state = ConnectionState.DEAD;
        }

        switch (state) {
            case TO_SEND_SYN -> {
                sendPacket(new SynPacket(nextSeqNumber));
                cleanState();
                state = ConnectionState.WAIT_SYN_ACK;
            }
            case WAIT_SYN_ACK -> {
                if (tick - tickMark > RESEND_TICK) {
                    if (waitRecord < RESEND_LIMIT) {
                        tickMark = tick;
                        ++waitRecord;
                        sendPacket(new SynPacket(nextSeqNumber));
                    } else {
                        deadReason = new IOException("Connection timed out");
                        state = ConnectionState.DEAD;
                    }
                }
            }
            case WAIT_SYN -> {
                if (tick - tickMark > TIMEOUT_TICK) {
                    deadReason = new IOException("Connection timed out");
                    state = ConnectionState.DEAD;
                }
            }
            case ACKED_SYN -> {
                if (tick - tickMark > IDLE_WAIT_TICK) {
                    cleanState();
                    state = ConnectionState.READY;
                }
            }
            case READY -> {
                try {
                    if (finalized && dataSendInputStream.available() == 0 && sendWindow.isEmpty()) {
                        cleanState();
                        state = ConnectionState.TO_SEND_FIN;
                        break;
                    }
                } catch (IOException ignored) {
                }

                boolean flag = false;
                try {
                    while (sendWindow.size() < windowSize && dataSendInputStream.available() > 0) {
                        flag = true;
                        byte[] data = new byte[Math.min(dataSendInputStream.available(), DATA_PACKET_SIZE)];
                        int read = dataSendInputStream.read(data);
                        var packet = new DataPacket(nextSeqNumber++, data, read);
                        sendWindow.add(packet);
                        sendPacket(packet);
                    }
                } catch (IOException ignored) {
                }
                if (flag) tickMark = tick;

                if (sendWindow.isEmpty()) {
                    tickMark = tick;
                    waitRecord = 0;
                } else {
                    if (tick - tickMark > RESEND_TICK) {
                        if (waitRecord < RESEND_LIMIT) {
                            tickMark = tick;
                            ++waitRecord;
                            for (var packet : sendWindow) sendPacket(packet);
                        } else {
                            deadReason = new IOException("Connection timed out");
                            state = ConnectionState.DEAD;
                        }
                    }
                }

                sendPacket(new AckPacket(nextSeqNumber, ackedNum));
            }
            case TO_SEND_FIN -> {
                sendPacket(new FinPacket(nextSeqNumber));

                cleanState();
                state = ConnectionState.WAIT_FIN_ACK;
            }
            case WAIT_FIN_ACK -> {
                if (!otherFinalized) sendPacket(new AckPacket(nextSeqNumber, ackedNum));
                if (tick - tickMark > RESEND_TICK) {
                    if (waitRecord < RESEND_LIMIT) {
                        tickMark = tick;
                        ++waitRecord;
                        sendPacket(new FinPacket(nextSeqNumber));
                    } else {
                        deadReason = new IOException("Connection timed out");
                        state = ConnectionState.DEAD;
                    }
                }
            }
            case WAIT_OTHER_FIN -> {
                if (otherFinalized) {
                    cleanState();
                    state = ConnectionState.LAST_WAIT;
                } else sendPacket(new AckPacket(nextSeqNumber, ackedNum));
            }
            case LAST_WAIT -> {
                if (tick - tickMark > IDLE_WAIT_TICK)
                    state = ConnectionState.DEAD;
            }
            case DEAD -> {
            }
            case null, default -> throw new IllegalStateException();
        }
    }

    @SuppressWarnings("BusyWait")
    protected void sendLoop() {
        while (state != ConnectionState.DEAD) {
            Packet packet;
            try {
                packet = finalized ? sendQueue.poll(25, TimeUnit.MILLISECONDS) : sendQueue.take();
            } catch (InterruptedException e) {
                continue;
            }
            if (packet == null) continue;

            int sent;
            try {
                sent = channel.send(packetEncoder.toByteBuffer(packet), peerAddress);
            } catch (IOException e) {
                deadReason = e;
                state = ConnectionState.DEAD;
                break;
            }

            if (sent == 0) {
                sendQueue.addFirst(packet);
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    protected void loop() {
        while (state != ConnectionState.DEAD) {
            Packet packet = tryRecvPacket();
            if (packet != null) {
                onRecvPacket(packet);
                continue;
            }

            onTick();
            ++tick;
            try {
                //noinspection BusyWait
                Thread.sleep(TICK_LENGTH);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public int read(byte[] buf) throws IOException {
        return read(buf, buf.length);
    }

    public int read(byte[] buf, int len) throws IOException {
        return dataReceiveInputStream.read(buf, 0, len);
    }

    public void write(byte[] data) throws IOException {
        write(data, data.length);
    }

    public void write(byte[] data, int len) throws IOException {
        if (finalized) throw new IOException("Connection finalized");
        dataSendOutputStream.write(data, 0, len);
    }

    public void disconnect() {
        finalized = true;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public boolean isDead() {
        return state == ConnectionState.DEAD;
    }

    public Exception getDeadReason() {
        return deadReason;
    }

    public void close() throws IOException {
        dataReceiveInputStream.close();
        dataReceiveOutputStream.close();
        dataSendInputStream.close();
        dataSendOutputStream.close();
        channel.close();
    }

    public enum ConnectionState {
        TO_SEND_SYN, WAIT_SYN_ACK, // Client
        WAIT_SYN, ACKED_SYN, // Server
        READY,
        TO_SEND_FIN, WAIT_FIN_ACK, WAIT_OTHER_FIN, LAST_WAIT, // FIN
        DEAD
    }
}
