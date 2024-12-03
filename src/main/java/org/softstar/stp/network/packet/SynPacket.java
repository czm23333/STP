package org.softstar.stp.network.packet;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class SynPacket extends Packet {
    public static byte TYPE = 0x01;

    public SynPacket(long seqNumber) {
        super(seqNumber);
    }

    public SynPacket(ByteBuffer buffer) {
        super(buffer);
    }

    @Override
    public void serialize(@NotNull ByteBuffer out) {
        super.serialize(out);
    }
}
