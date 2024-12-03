package org.softstar.stp.network.connection;

import org.jetbrains.annotations.NotNull;
import org.softstar.stp.network.packet.Packet;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

public class DebuggingConnection extends Connection {
    private final String name;

    public DebuggingConnection(String name, DatagramChannel channel, SocketAddress peerAddress, boolean isServer) throws IOException {
        super(channel, peerAddress, isServer);
        this.name = name;
    }

    @Override
    protected void sendPacket(@NotNull Packet packet) {
        boolean sent = RANDOM.nextBoolean();
        if (sent) super.sendPacket(packet);
        System.out.printf("[%s] sendPacket: %s (sent=%b)%n", name, packet, sent);
    }
}
