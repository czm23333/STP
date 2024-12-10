package org.softstar.stp.network.connection;

import org.jetbrains.annotations.NotNull;
import org.softstar.stp.network.decoder.CRC32PacketDecoder;
import org.softstar.stp.network.encoder.DebuggingEncoder;
import org.softstar.stp.network.packet.Packet;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

public class DebuggingConnection extends Connection {
    private static final double DROP_RATE = 0.1;
    private static final double CORRUPT_RATE = 0.0001;
    private static final double REORDER_RATE = 0.1;

    private final String name;

    public DebuggingConnection(String name, DatagramChannel channel, SocketAddress peerAddress, boolean isServer) throws IOException {
        super(channel, peerAddress, isServer, new DebuggingEncoder(name, RANDOM, CORRUPT_RATE), new CRC32PacketDecoder());
        this.name = name;
    }

    @Override
    protected void sendPacket(@NotNull Packet packet) {
        boolean drop = RANDOM.nextDouble() <= DROP_RATE;
        if (drop) {
            System.out.printf("[%s] drop packet: %s%n", name, packet);
            return;
        }

        boolean reorder = RANDOM.nextDouble() <= REORDER_RATE;
        if (reorder) {
            System.out.printf("[%s] reorder packet: %s%n", name, packet);
            sendQueue.offerFirst(packet);
        } else {
            System.out.printf("[%s] send packet: %s%n", name, packet);
            super.sendPacket(packet);
        }
    }
}
