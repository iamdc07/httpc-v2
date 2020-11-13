import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HandleThreads extends Thread {
    RequestParameters requestParameters;
    InetSocketAddress serverAddress;
    SocketAddress routerAddress;

    public HandleThreads(RequestParameters requestParameters, InetSocketAddress serverAddress, SocketAddress routerAddress) {
        this.requestParameters = requestParameters;
        this.serverAddress = serverAddress;
        this.routerAddress = routerAddress;
    }

    public void run(){
        Charset utf8 = StandardCharsets.UTF_8;
        HttpRequest httpRequest = new HttpRequest();
        String response = "";
        boolean redirect = false;
        HttpResponse httpResponse = new HttpResponse();
        String payload = httpRequest.processRequest(this.requestParameters);
        ByteBuffer byteBuffer = utf8.encode(payload);

        try {
            SocketChannel socketChannel = SocketChannel.open();
//            socketChannel.connect(socketAddress);
            socketChannel.write(byteBuffer);
            byteBuffer.clear();

            // Receiving response from the server
            while (byteBuffer.hasRemaining()) {
                int length = socketChannel.read(byteBuffer);

                if (length == -1)
                    break;

                byteBuffer.flip();

                String lines = String.valueOf(utf8.decode(byteBuffer));
                response = response.concat(lines);
                byteBuffer.compact();
            }

            byteBuffer.clear();
//            socketChannel.close();

            if (httpResponse.processResponse(response, this.requestParameters)) {
                this.requestParameters.requestLine = this.requestParameters.redirectionUrl;
                redirect = true;
                System.out.println("\nREDIRECTING...\n");
            }
            response = "";
            System.out.println("--------------------------------------------------------------------------");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
