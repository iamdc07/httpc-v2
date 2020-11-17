import model.Packet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import static java.nio.channels.SelectionKey.OP_READ;

public class Client {
    static long currentSequence = 0L;
    static Set<Long> sequenceNumbers = new HashSet<>();
    static int start = 1;
    public static void main(String[] args) throws IOException, InterruptedException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        HttpResponse httpResponse = new HttpResponse();
        ArrayBlockingQueue<Long> ackBuffer = new ArrayBlockingQueue<>(99);
//        ArrayList<TimerTask> tasks = new ArrayList<>();
        HashMap<Long, TimerTask> tasksMap = new HashMap<>();
//        ArrayBlockingQueue<Packet> responsePackets = new ArrayBlockingQueue<>(99);
        List<Packet> receivedPackets = Collections.synchronizedList(new ArrayList<>());
//        Set<Long> sequenceNumbers = new HashSet<>();
//        Charset utf8 = StandardCharsets.UTF_8;
        RequestParameters requestParameters = null;
        Packet finalPacket = null;
        String input = "", choice = "";
        boolean redirect = false;


        System.out.print("Type in 'httpc Command' or 'httpc help' to get started!");

        while (true) {
            System.out.println("\n");
            if (!redirect) {
                System.out.println("Enter your choice:");
                System.out.println("1. Multithreaded Requests");
                System.out.println("2. Single Request");
                choice = bufferedReader.readLine();

                System.out.println("Enter your command");
                input = bufferedReader.readLine();
            }

            if (input.startsWith("httpc help")) {
                displayUsage(input);
            } else {
                if (!redirect)
                    requestParameters = validate(input);
                else
                    redirect = false;

                if (requestParameters.isValid && choice.equalsIgnoreCase("1")) {
                    invokeThreads(requestParameters);
                    continue;
                }

                if (requestParameters.isValid) {
                    HttpRequest httpRequest = new HttpRequest();

                    try (DatagramChannel channel = DatagramChannel.open()) {
                        InetSocketAddress serverAddress = new InetSocketAddress(requestParameters.host, requestParameters.port);
                        SocketAddress routerAddress = new InetSocketAddress(requestParameters.routerHost, requestParameters.routerPort);
//                        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);
//                        SocketAddress routerAddress = new InetSocketAddress(routerHost, routerPort);

                        // TO-DO Have a 3-way Handshake here
                        // TO-DO Make it reliable

                        HashMap<Long, Packet> packetList = generatePackets(httpRequest, requestParameters, serverAddress);

//                        Packet packet = new Packet.Builder()
//                                .setType(0)
//                                .setSequenceNumber(1L)
//                                .setPortNumber(serverAddress.getPort())
//                                .setPeerAddress(serverAddress.getAddress())
//                                .setPayload(payload.getBytes())
//                                .create();

                        // Send each packet one by one
//                        for (Packet each : packetList) {
//                            channel.send(each.toBuffer(), routerAddress);
//                            // Packet Sent
//                        }
                        boolean flag = true;

                        Timer timer = new Timer(true);

                        // Send Packets
                        while (flag) {
                            flag = sendPackets(packetList, channel, routerAddress, timer, tasksMap);
                            System.out.println("BEFORE START:" + Client.start);
                            System.out.println("AFTER START:" + Client.start);

                            int k = 0;

                            // Create Buffer for Response
                            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);


                            // Receive each Ack one by one
                            while (k < 5) {
                                channel.receive(buf);
                                buf.flip();

//                                // Condition for final(Last) packet [Needs to be modified]
//                                if (buf.limit() < Packet.MIN_LEN || buf.limit() > Packet.MAX_LEN)
//                                    break;

                                Packet ackPacket = Packet.fromBuffer(buf);
                                System.out.println("BUFFER SIZE:" + buf.limit());
                                buf.clear();

                                if (!ackBuffer.contains(ackPacket.getSequenceNumber())) {
                                    ackBuffer.add(ackPacket.getSequenceNumber());

                                    System.out.println("ACK BUFFER SIZE:" + ackBuffer.size());
                                    System.out.println("TASKS SIZE:" + tasksMap.size());
                                    System.out.println("ACK SEQ:" + ((int) ackPacket.getSequenceNumber()));

                                    if (tasksMap.containsKey(ackPacket.getSequenceNumber())) {
                                        TimerTask timerTask = tasksMap.get(ackPacket.getSequenceNumber());
                                        timerTask.cancel();
                                        tasksMap.remove(ackPacket.getSequenceNumber());
                                        System.out.println("CANCELLED TASK:" + (ackPacket.getSequenceNumber()));
                                    }
                                }

                                if (tasksMap.size() == 0) {
                                    break;
                                }

//                                packetList.add(responsePacket);
                                System.out.println("Received Ack List Size:" + ackBuffer.size());
//                                // Wait for response
//                                selector.select(5000);
//
//                                keys = selector.selectedKeys();
//                                if (keys.isEmpty()) {
//                                    System.out.println("No response after timeout");
//                                    break;
//                                }
                                k++;
                            }
//                            start += 5;
//                            start = (int) currentSequence;
                        }
//                        packetList.clear();
                        timer.cancel();

//                        // Receive a packet within the timeout
//                        channel.configureBlocking(false);
//                        Selector selector = Selector.open();
//                        channel.register(selector, OP_READ);
//                        // Waiting for the response
//                        selector.select(5000);
//
//                        Set<SelectionKey> keys = selector.selectedKeys();
//                        if (keys.isEmpty()) {
//                            System.out.println("No response after timeout");
//                            continue;
//                        }

                        // Create Buffer for Response
//                        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
//
//                        // Receive each packet one by one
//                        while (true) {
//                            channel.receive(buf);
//                            buf.flip();
//
//                            if (buf.limit() < Packet.MIN_LEN || buf.limit() > Packet.MAX_LEN)
//                                break;
//
//                            Packet responsePacket = Packet.fromBuffer(buf);
//                            buf.clear();
//                            packetList.add(responsePacket);
//                            System.out.println("Received ArrayList Size:" + packetList.size());
//                            // Wait for response
//                            selector.select(5000);
//
//                            keys = selector.selectedKeys();
//                            if (keys.isEmpty()) {
//                                System.out.println("No response after timeout");
//                                break;
//                            }
//                        }
                        flag = true;
//                        start = 0;

                        // Create Buffer for Response
                        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);

                        // Receive Response
                        while (flag) {
                            channel.receive(buf);
                            buf.flip();

//                            // Condition for final(Last) packet [Needs to be modified]
//                            if (buf.limit() < Packet.MIN_LEN || buf.limit() > Packet.MAX_LEN)
//                                break;

                            Packet responsePacket = Packet.fromBuffer(buf);
                            buf.clear();

//                            if (responsePacket.getSequenceNumber() < currentSequence - start || responsePacket.getSequenceNumber() > currentSequence - start)

                            if (!sequenceNumbers.contains(responsePacket.getSequenceNumber())) {
//                                sequenceNumbers.add(responsePacket.getSequenceNumber());
                                System.out.println("Received Response PACK Seq number:" + responsePacket.getSequenceNumber());
//                                System.out.println("Current Sequence Pointer:" + currentSequence);

                                // TO-DO Add ack resend - Drop and delay
                                // Send ACK for the received packet
                                Packet packet = responsePacket.toBuilder()
                                        .setType(2)
                                        .setPayload(new byte[0])
                                        .create();

                                channel.send(packet.toBuffer(), routerAddress);
                                modifySequence(true);

                                String responsePayload = new String(responsePacket.getPayload(), StandardCharsets.UTF_8);

                                if (responsePayload.equalsIgnoreCase("\r\n"))
                                    finalPacket = responsePacket;
                                else {
                                    // Add packet to buffer
                                    receivedPackets.add(responsePacket);
                                }
                                sequenceNumbers.add(responsePacket.getSequenceNumber());
                            }

                            start = (int) currentSequence;
                            ++Client.start;

//                                packetList.add(responsePacket);
                            System.out.println("Received Response List Size:" + receivedPackets.size());
                            System.out.println("Received Response Payload size:" + responsePacket.getPayload().length);


                            if (finalPacket != null && sequenceNumbers.size() == (finalPacket.getSequenceNumber())) {
                                flag = false;
                            }

//                            start += 5;
//                            start = (int) currentSequence;
                        }

                        String response = "";

                        synchronized (receivedPackets) {
                            Collections.sort(receivedPackets, new Comparator<>() {
                                @Override
                                public int compare(Packet o1, Packet o2) {
                                    return Long.compare(o1.getSequenceNumber(), o2.getSequenceNumber());
                                }
                            });
                        }

                        for (Packet each : receivedPackets) {
                            System.out.println("Response Packet Seq " + each.getSequenceNumber());
                            String responsePayload = new String(each.getPayload(), StandardCharsets.UTF_8);
                            response = response.concat(responsePayload);
                        }
//                        logger.info("Packet: {}", resp);
//                        logger.info("Router: {}", router);

//                        String responsePayload = new String(packetList.get(0).getPayload(), StandardCharsets.UTF_8);
//                        logger.info("Payload: {}",  payload);

                        if (httpResponse.processResponse(response, requestParameters)) {
                            requestParameters.requestLine = requestParameters.redirectionUrl;
                            redirect = true;
                            System.out.println("\nREDIRECTING...\n");
                        }

//                        keys.clear();
                        packetList.clear();
                        ackBuffer.clear();
//                        sequenceNumbers.clear();
                        receivedPackets.clear();
                        finalPacket = null;
//                        start = 0;
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
//                    try {
//
//                        SocketChannel socketChannel = SocketChannel.open();
//                        socketChannel.connect(socketAddress);
//                        socketChannel.write(byteBuffer);
//                        byteBuffer.clear();
//
//                        // Receiving response from the server
//                        while (byteBuffer.hasRemaining()) {
//                            int length = socketChannel.read(byteBuffer);
//
//                            if (length == -1)
//                                break;
//
//                            byteBuffer.flip();
//
//                            String lines = String.valueOf(utf8.decode(byteBuffer));
//                            response = response.concat(lines);
//                            byteBuffer.compact();
//                        }
//
//                        byteBuffer.clear();
//                        socketChannel.close();
//
//                        if (httpResponse.processResponse(response, requestParameters)) {
//                            requestParameters.requestLine = requestParameters.redirectionUrl;
//                            redirect = true;
//                            System.out.println("\nREDIRECTING...\n");
//                        }
//                        response = "";
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
                } else {
                    System.out.println("Your input is invalid, please try again!");
                }
            }
        }
    }

    private static void invokeThreads(RequestParameters requestParameters) throws InterruptedException, IOException {
//        SocketAddress socketAddress = new InetSocketAddress(requestParameters.host, 8080);
        InetSocketAddress serverAddress = new InetSocketAddress(requestParameters.host, requestParameters.port);
        SocketAddress routerAddress = new InetSocketAddress(requestParameters.routerHost, requestParameters.routerPort);

        for (int i = 0; i < 10; i++) {
            HandleThreads handleThreads = new HandleThreads(requestParameters, serverAddress, routerAddress);
            handleThreads.start();
        }
    }

    private static HashMap<Long, Packet> generatePackets(HttpRequest httpRequest, RequestParameters requestParameters, InetSocketAddress serverAddress) {
        HashMap<Long, Packet> packetList = new HashMap<Long, Packet>();
        String payload = httpRequest.processRequest(requestParameters);
        byte[] buffer = payload.getBytes();
//        long seq = 1L;
        byte[] bytes;
        bytes = "\r\n".getBytes();
        for (int i = 0; i < buffer.length; i = i + 1013) {
//            byte[] bytes = payload.getBytes(i, i + 1012);
            byte[] slice = Arrays.copyOfRange(buffer, i, i + 1012);

            Packet packet = new Packet.Builder()
                    .setType(0)
                    .setSequenceNumber(modifySequence(true))
                    .setPortNumber(serverAddress.getPort())
                    .setPeerAddress(serverAddress.getAddress())
                    .setPayload(slice)
                    .create();

            packetList.put(currentSequence, packet);
            sequenceNumbers.add(currentSequence);
//            seq++;
        }

        Packet lastPacket = new Packet.Builder()
                .setPeerAddress(serverAddress.getAddress())
                .setPortNumber(serverAddress.getPort())
                .setSequenceNumber(modifySequence(true))
                .setType(2)
                .setPayload(bytes)
                .create();

        packetList.put(currentSequence, lastPacket);
        sequenceNumbers.add(currentSequence);

        return packetList;
    }

    private static boolean sendPackets(HashMap<Long, Packet> packetList, DatagramChannel channel, SocketAddress routerAddress, Timer timer, HashMap<Long, TimerTask> tasks) throws IOException {
        System.out.println("Start:" + Client.start);
        System.out.println("Current Sequence:" + Client.currentSequence);
        for (int i = start; i < start + 5; i++) {
            System.out.println("SENDING PACKET NO: " + (modifySequence(false)));
            if (packetList.size() == 1) {
                channel.send(packetList.get((long) i).toBuffer(), routerAddress);
                TimerTask task = new PacketTimeout(packetList.get((long) i), channel, routerAddress);
                tasks.put((long) i, task);
                timer.schedule(task, 5000, 5000);
                packetList.remove((long) i);
                Client.start++;
                return false;
            }

            channel.send(packetList.get((long) i).toBuffer(), routerAddress);
            TimerTask task = new PacketTimeout(packetList.get((long) i), channel, routerAddress);
            tasks.put((long) i, task);
            timer.schedule(task, 5000, 5000);
            packetList.remove((long) i);
            Client.start++;
        }

        return true;
    }

    protected static long modifySequence(boolean modify) {
        if (modify)
            currentSequence++;

        return currentSequence;
    }

    @SuppressWarnings("Duplicates")
    private static RequestParameters validate(String input) {
        RequestParameters requestParameters = new RequestParameters();
        String[] words = input.split(" ");

        if (words[0].equalsIgnoreCase("httpc")) {

            for (int i = 1; i < words.length; i++) {
                if (words[i].startsWith("http"))
                    continue;

                switch (words[i]) {
                    case "get":
                        requestParameters.isGetRequest = true;
                        break;
                    case "post":
                        requestParameters.isPostRequest = true;
                        break;
                    case "-v":
                        requestParameters.isVerbose = true;
                        requestParameters.indexVerbose = input.indexOf("-v");
                        break;
                    case "-h":
                        requestParameters.hasHeaders = true;
                        requestParameters.headers.add(words[i + 1]);
                        i++;
                        break;
                    case "-d":
                    case "--d":
                        requestParameters.isInline = true;
                        requestParameters.data = words[i + 1];
                        i++;
                        break;
                    case "-f":
                        requestParameters.readFromFile = true;
                        requestParameters.inputFile = words[i + 1];
                        i++;
                        break;
                    case "-o":
                        requestParameters.writeToFile = true;
                        requestParameters.outputFile = words[i + 1];
                        requestParameters.requestLine = words[words.length - 3];
                        i++;
                        break;
                    default:
                        return requestParameters;
                }
            }

            if (!requestParameters.writeToFile)
                requestParameters.requestLine = words[words.length - 1];

            String[] temp = requestParameters.requestLine.split(":");


            if (temp.length == 3) {
                int index = temp[2].indexOf(":");
                requestParameters.port = Integer.parseInt(temp[2].substring(index + 1, index + 5));
            }

            if (!requestParameters.requestLine.contains("http")) {
                System.out.println("Please enter a valid URL");
                return requestParameters;
            }

            if (requestParameters.isGetRequest) {
                if (requestParameters.isInline || requestParameters.readFromFile)
                    return requestParameters;
                else
                    requestParameters.isValid = true;
            } else if (requestParameters.isPostRequest) {
                if (!requestParameters.isInline && !requestParameters.readFromFile) {
                    System.out.println("Please enter either -d or -f command");
                    return requestParameters;
                } else if (requestParameters.isInline && requestParameters.readFromFile) {
                    System.out.println("Please enter either -d or -f command");
                    return requestParameters;
                } else
                    requestParameters.isValid = true;
            } else {
                if (requestParameters.writeToFile) {
                    if (words[words.length - 3].contains("get")) {
                        requestParameters.isGetRequest = true;

                        if (requestParameters.isInline || requestParameters.readFromFile)
                            return requestParameters;
                        else
                            requestParameters.isValid = true;
                    } else if ((words[words.length - 3].contains("post"))) {
                        requestParameters.isPostRequest = true;

                        if (!requestParameters.isInline && !requestParameters.readFromFile) {
                            System.out.println("Please enter either -d or -f command");
                            return requestParameters;
                        } else if (requestParameters.isInline && requestParameters.readFromFile) {
                            System.out.println("Please enter either -d or -f command");
                            return requestParameters;
                        } else
                            requestParameters.isValid = true;
                    }
                }
            }
        }

        return requestParameters;
    }

    private static void displayUsage(String input) {
        String[] words = input.split(" ");

        if (words.length == 2) {
            System.out.println("\nhttpc is a curl-like application but supports HTTP protocol only.");
            System.out.println("Usage:");
            System.out.println("httpc (get|post) [-v] (-h \"k:v\")* [-d inline-data] [-f file] URL\n");
            System.out.println("The commands are:");
            System.out.println("get \t executes a HTTP GET request and prints the response.");
            System.out.println("post \t executes a HTTP POST request and prints the response.");
            System.out.println("help \t prints this screen.");
            System.out.println("Use \"httpc help [command]\" for more information about a command.\nhttpc help");
        } else if (words[words.length - 1].equalsIgnoreCase("get")) {
            System.out.println("\nusage: httpc get [-v] [-h key:value] URL\n");
            System.out.println("Get executes a HTTP GET request for a given URL.\n");
            System.out.println("   -v             Prints the detail of the response such as protocol, status, and headers.");
            System.out.println("   -h key:value   Associates headers to HTTP Request with the format \'key:value\'.\n");
        } else if (words[words.length - 1].equalsIgnoreCase("post")) {
            System.out.println("\nUsage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n");
            System.out.println("Post executes a HTTP POST request for a given URL with inline data or from file.\n");
            System.out.println("\t-v             Prints the detail of the response such as protocol, status, and headers.");
            System.out.println("\t-h key:value   Associates headers to HTTP Request with the format \'key:value\'.");
            System.out.println("\t-d string      Associates an inline data to the body HTTP POST request.");
            System.out.println("\t-f file        Associates the content of a file to the body HTTP POST request.\n");
            System.out.println("Either [-d] or [-f] can be used but not both.\n");
        }
    }
}
