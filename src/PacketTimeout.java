import model.Packet;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.TimerTask;

public class PacketTimeout extends TimerTask {
    Packet packet;
    DatagramChannel channel;
    SocketAddress routerAddress;
    int i = 0;

    PacketTimeout(Packet packet, DatagramChannel channel, SocketAddress routerAddress) {
        this.packet = packet;
        this.channel = channel;
        this.routerAddress = routerAddress;
    }

    @Override
    public void run() {
        try {
            System.out.println("Resending Packet number: " + packet.getSequenceNumber());
            channel.send(packet.toBuffer(), routerAddress);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
