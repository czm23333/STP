package org.softstar.stp.network.packet;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public abstract class Packet {
    private final long seqNumber;

    public Packet(long seqNumber) {
        this.seqNumber = seqNumber;
    }

    public Packet(ByteBuffer buffer) {
        this.seqNumber = buffer.getLong();
    }

    public long getSeqNumber() {
        return seqNumber;
    }

    public void serialize(@NotNull ByteBuffer out) {
        out.putLong(seqNumber);
    }
}
