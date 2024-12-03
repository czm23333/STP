package org.softstar.stp.network.decoder;

import org.jetbrains.annotations.NotNull;
import org.softstar.stp.network.packet.*;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Function;

public class BasicPacketDecoder extends AbstractPacketDecoder {
    private static final Map<Byte, Function<ByteBuffer, Packet>> CONSTRUCTORS = Map.of(
            SynPacket.TYPE, SynPacket::new,
            SynAckPacket.TYPE, SynAckPacket::new,
            DataPacket.TYPE, DataPacket::new,
            AckPacket.TYPE, AckPacket::new,
            FinPacket.TYPE, FinPacket::new,
            FinAckPacket.TYPE, FinAckPacket::new
    );

    @Override
    @NotNull
    public Packet fromByteBuffer(@NotNull ByteBuffer buffer) {
        byte type = buffer.get();
        var constructor = CONSTRUCTORS.get(type);
        if (constructor == null) throw new IllegalArgumentException("Unknown packet type: " + type);
        return constructor.apply(buffer);
    }
}
