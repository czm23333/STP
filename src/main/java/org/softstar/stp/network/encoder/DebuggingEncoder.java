package org.softstar.stp.network.encoder;

import org.jetbrains.annotations.NotNull;
import org.softstar.stp.network.packet.Packet;

import java.nio.ByteBuffer;
import java.util.Random;

public class DebuggingEncoder extends CRC32PacketEncoder {
    private final String name;
    private final Random random;
    private final double byteCorruptRate;

    public DebuggingEncoder(String name, Random random, double byteCorruptRate) {
        this.name = name;
        this.random = random;
        this.byteCorruptRate = byteCorruptRate;
    }

    @Override
    public @NotNull ByteBuffer toByteBuffer(@NotNull Packet packet) {
        ByteBuffer res = super.toByteBuffer(packet);
        boolean overallCorrupt = false;
        byte[] buf = new byte[1];
        for (int i = 0; i < res.limit(); ++i) {
            boolean corrupt = random.nextDouble() <= byteCorruptRate;
            if (!corrupt) continue;
            overallCorrupt = true;
            random.nextBytes(buf);
            res.put(i, buf[0]);
        }
        if (overallCorrupt) System.out.printf("[%s] corrupt packet: %s%n", name, packet);
        return res;
    }
}
