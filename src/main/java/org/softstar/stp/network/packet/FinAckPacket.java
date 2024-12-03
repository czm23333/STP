package org.softstar.stp.network.packet;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class FinAckPacket extends Packet {
    public static byte TYPE = 0x06;

    public FinAckPacket(long seqNumber) {
        super(seqNumber);
    }

    public FinAckPacket(ByteBuffer buffer) {
        super(buffer);
    }

    @Override
    public void serialize(@NotNull ByteBuffer out) {
        super.serialize(out);
    }
}
