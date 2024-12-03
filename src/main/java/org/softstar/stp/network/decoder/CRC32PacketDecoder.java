package org.softstar.stp.network.decoder;

import org.jetbrains.annotations.NotNull;
import org.softstar.stp.exception.CorruptedPacketException;
import org.softstar.stp.network.packet.Packet;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class CRC32PacketDecoder extends BasicPacketDecoder {
    @Override
    public @NotNull Packet fromByteBuffer(@NotNull ByteBuffer buffer) {
        int crc32 = buffer.getInt();
        CRC32 crc = new CRC32();
        crc.update(buffer);
        if (crc32 != (int) crc.getValue()) throw new CorruptedPacketException();
        buffer.position(4);
        return super.fromByteBuffer(buffer);
    }
}
