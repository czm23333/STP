package org.softstar.stp.network.packet;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class AckPacket extends Packet {
    public static byte TYPE = 0x04;

    private final long ackNumber;

    public AckPacket(long seqNumber, long ackNumber) {
        super(seqNumber);
        this.ackNumber = ackNumber;
    }

    public AckPacket(ByteBuffer buffer) {
        super(buffer);
        this.ackNumber = buffer.getLong();
    }

    @Override
    public void serialize(@NotNull ByteBuffer out) {
        super.serialize(out);
        out.putLong(getAckNumber());
    }

    public long getAckNumber() {
        return ackNumber;
    }
}
