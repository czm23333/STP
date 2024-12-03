package org.softstar.stp;

import org.softstar.stp.network.connection.Connection;
import org.softstar.stp.network.connection.DebuggingConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

public class Main {
    private static String produceTestString(int param) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < param; i++) sb.append(i);
        return sb.toString();
    }

    private static void startReadLoop(String name, Connection conn) {
        new Thread(() -> {
            while (true) {
                byte[] buffer = new byte[64];
                int read;
                try {
                    read = conn.read(buffer);
                } catch (IOException e) {
                    continue;
                }
                if (read > 0) System.out.printf("[%s] Received: %s%n", name, new String(buffer, 0, read));
                else {
                    try {
                        //noinspection BusyWait
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }).start();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        DatagramChannel channelClient = DatagramChannel.open();
        DatagramChannel channelServer = DatagramChannel.open();
        SocketAddress clientAddress = new InetSocketAddress("127.0.0.1", 3890);
        SocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 3891);
        channelClient.bind(clientAddress);
        channelServer.bind(serverAddress);
        channelClient.connect(serverAddress);
        channelServer.connect(clientAddress);

        try (Connection connClient = new DebuggingConnection("client", channelClient, serverAddress, false);
             Connection connServer = new DebuggingConnection("server", channelServer, clientAddress, true)) {
            startReadLoop("client", connClient);
            startReadLoop("server", connServer);
            connClient.write(("helloServer" + produceTestString(25565) + "byeServer").getBytes());
            connServer.write(("helloClient" + produceTestString(25565) + "byeClient").getBytes());
            connClient.disconnect();
            Thread.sleep(10000);
            System.out.printf("Client dead %b %s%n", connClient.isDead(), connClient.getDeadReason());
            System.out.printf("Server dead %b %s%n", connServer.isDead(), connServer.getDeadReason());
        }
    }
}