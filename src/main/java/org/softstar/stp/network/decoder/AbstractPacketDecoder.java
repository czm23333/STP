package org.softstar.stp.network.decoder;

import org.jetbrains.annotations.NotNull;
import org.softstar.stp.network.packet.Packet;

import java.nio.ByteBuffer;

public abstract class AbstractPacketDecoder {
    @NotNull
    public Packet fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return fromByteBuffer(buffer);
    }

    @NotNull
    public abstract Packet fromByteBuffer(@NotNull ByteBuffer buffer);
}
