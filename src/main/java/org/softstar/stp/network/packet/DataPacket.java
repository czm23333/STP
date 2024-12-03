package org.softstar.stp.network.packet;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DataPacket extends Packet {
    public static byte TYPE = 0x03;

    private final byte[] data;

    public DataPacket(long seqNumber, byte[] data) {
        this(seqNumber, data, data.length);
    }

    public DataPacket(long seqNumber, byte[] data, int length) {
        super(seqNumber);
        this.data = Arrays.copyOf(data, length);
    }

    public DataPacket(ByteBuffer buffer) {
        super(buffer);
        this.data = new byte[buffer.remaining()];
        buffer.get(getData());
    }

    @Override
    public void serialize(@NotNull ByteBuffer out) {
        super.serialize(out);
        out.put(getData());
    }

    public byte[] getData() {
        return data;
    }
}
