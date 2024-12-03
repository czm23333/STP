package org.softstar.stp.network.encoder;

import org.jetbrains.annotations.NotNull;
import org.softstar.stp.network.packet.Packet;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class CRC32PacketEncoder extends BasicPacketEncoder {
    @Override
    public @NotNull ByteBuffer toByteBuffer(@NotNull Packet packet) {
        var encoded = super.toByteBuffer(packet);
        CRC32 crc32 = new CRC32();
        crc32.update(encoded);
        encoded.rewind();
        var res = ByteBuffer.allocate(65535);
        res.putInt((int) crc32.getValue());
        res.put(encoded);
        res.limit(res.position());
        res.rewind();
        return res;
    }
}
