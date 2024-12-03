package org.softstar.stp.network.encoder;

import org.jetbrains.annotations.NotNull;
import org.softstar.stp.network.packet.Packet;

import java.nio.ByteBuffer;

public abstract class AbstractPacketEncoder {
    public byte[] toBytes(@NotNull Packet packet) {
        var buffer = toByteBuffer(packet);
        if (buffer.hasArray()) return buffer.array();
        byte[] array = new byte[buffer.remaining()];
        buffer.get(array);
        return array;
    }

    @NotNull
    public abstract ByteBuffer toByteBuffer(@NotNull Packet packet);
}
