package org.softstar.stp.network.packet;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class SynAckPacket extends Packet {
    public static byte TYPE = 0x02;

    public SynAckPacket(long seqNumber) {
        super(seqNumber);
    }

    public SynAckPacket(ByteBuffer buffer) {
        super(buffer);
    }

    @Override
    public void serialize(@NotNull ByteBuffer out) {
        super.serialize(out);
    }
}
