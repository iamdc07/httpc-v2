import model.Packet;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

public class ChannelHelper extends Thread {
    SocketAddress routerAddress;
    DatagramChannel channel;
    int i = 0;

    public ChannelHelper(DatagramChannel channel, SocketAddress routerAddress) {
        this.routerAddress = routerAddress;
        this.channel = channel;
    }

    @Override
    public void run() {
        try {
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);

            Set<SelectionKey> keys = selector.selectedKeys();
            System.out.println("Selector Keys Size:" + keys.size());
            Iterator<SelectionKey> iterator = keys.iterator();
//            SelectionKey key = iterator.next();

            System.out.println("I:" + i);
            while (i < 2) {
                selector.select(5000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                if (!iter.hasNext())
                    i++;

                while (iter.hasNext()) {

                    SelectionKey key = iter.next();

                    if (key.isReadable()) {
                        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);

                        channel.receive(buf);
                        buf.flip();

                        Packet packet = Packet.fromBuffer(buf);
                        System.out.println("BUF LIMIT AND SEQ: " + buf.limit() + " ");

                        Packet ackPacket = packet.toBuilder()
                                .setType(2)
                                .setPayload(new byte[0])
                                .create();

                        System.out.println("ACKED PACKET:" + ackPacket.getSequenceNumber());

                        channel.send(ackPacket.toBuffer(), routerAddress);
                    }
                    iter.remove();
                }
            }

            keys.clear();
            selector.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
