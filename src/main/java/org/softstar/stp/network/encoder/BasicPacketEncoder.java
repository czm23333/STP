package org.softstar.stp.network.encoder;

import org.jetbrains.annotations.NotNull;
import org.softstar.stp.network.packet.*;

import java.nio.ByteBuffer;
import java.util.Map;

public class BasicPacketEncoder extends AbstractPacketEncoder {
    private static final Map<Class<? extends Packet>, Byte> TYPES = Map.of(
            SynPacket.class, SynPacket.TYPE,
            SynAckPacket.class, SynAckPacket.TYPE,
            DataPacket.class, DataPacket.TYPE,
            AckPacket.class, AckPacket.TYPE,
            FinPacket.class, FinPacket.TYPE,
            FinAckPacket.class, FinAckPacket.TYPE
    );

    @Override
    public @NotNull ByteBuffer toByteBuffer(@NotNull Packet packet) {
        Byte type = TYPES.get(packet.getClass());
        if (type == null)
            throw new IllegalArgumentException(String.format("Packet %s is not supported", packet.getClass()));
        var res = ByteBuffer.allocate(65535);
        res.put(type);
        packet.serialize(res);
        res.limit(res.position());
        res.rewind();
        return res;
    }
}
