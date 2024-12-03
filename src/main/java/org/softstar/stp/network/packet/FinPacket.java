package org.softstar.stp.network.packet;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class FinPacket extends Packet {
    public static byte TYPE = 0x05;

    public FinPacket(long seqNumber) {
        super(seqNumber);
    }

    public FinPacket(ByteBuffer buffer) {
        super(buffer);
    }

    @Override
    public void serialize(@NotNull ByteBuffer out) {
        super.serialize(out);
    }
}
