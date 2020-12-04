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
    static int start = 2;
    static boolean firstExecution = true;

    public static void main(String[] args) throws IOException, InterruptedException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        HttpResponse httpResponse = new HttpResponse();
        ArrayBlockingQueue<Long> ackBuffer = new ArrayBlockingQueue<>(99);
        HashMap<Long, TimerTask> sendTasksMap = new HashMap<>();
        HashMap<Long, TimerTask> ackTasksMap = new HashMap<>();
        List<Packet> receivedPackets = Collections.synchronizedList(new ArrayList<>());
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

                    if (firstExecution) {
                        // 3-way Handshake
                        Packet packet = connect(requestParameters);
                        receivedPackets.add(packet);
                        firstExecution = false;
                    }

                    try (DatagramChannel channel = DatagramChannel.open()) {
                        InetSocketAddress serverAddress = new InetSocketAddress(requestParameters.host, requestParameters.port);
                        SocketAddress routerAddress = new InetSocketAddress(requestParameters.routerHost, requestParameters.routerPort);

                        HashMap<Long, Packet> packetList = generatePackets(httpRequest, requestParameters, serverAddress);

                        boolean flag = true;

                        Timer timer = new Timer(true);

                        // Send Packets
                        while (flag) {
                            flag = sendPackets(packetList, channel, routerAddress, timer, sendTasksMap);
                            System.out.println("BEFORE START:" + Client.start);
                            System.out.println("AFTER START:" + Client.start);

                            int k = 0;

                            // Create Buffer for Response
                            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);


                            // Receive each Ack one by one
                            while (sendTasksMap.size() != 0) {
                                channel.receive(buf);
                                buf.flip();

//                                // Condition for final(Last) packet [Needs to be modified]
//                                if (buf.limit() < Packet.MIN_LEN || buf.limit() > Packet.MAX_LEN)
//                                    break;

                                Packet ackPacket = Packet.fromBuffer(buf);
                                System.out.println("BUFFER SIZE:" + buf.limit());
                                buf.clear();

                                if (!ackBuffer.contains(ackPacket.getSequenceNumber()) && ackPacket.getType() == 2) {
                                    ackBuffer.add(ackPacket.getSequenceNumber());

                                    System.out.println("ACK BUFFER SIZE:" + ackBuffer.size());
                                    System.out.println("SENT PACKETS TASKS SIZE:" + sendTasksMap.size());
                                    System.out.println("RECEIVED ACK SEQ:" + ((int) ackPacket.getSequenceNumber()));

                                    if (sendTasksMap.containsKey(ackPacket.getSequenceNumber())) {
                                        TimerTask timerTask = sendTasksMap.get(ackPacket.getSequenceNumber());
                                        timerTask.cancel();
                                        sendTasksMap.remove(ackPacket.getSequenceNumber());
                                        System.out.println("CANCELLED TASK:" + (ackPacket.getSequenceNumber()));
                                    }
                                }
                            }
                        }

                        timer.cancel();
                        timer.purge();

                        flag = true;

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

                            // Check if the packet received is duplicate
                            if (!sequenceNumbers.contains(responsePacket.getSequenceNumber()) && responsePacket.getType() == 0) {
                                System.out.println("Received Response Packet:" + responsePacket.getSequenceNumber());

                                modifySequence(true);
                                System.out.println("Current Sequence Pointer:" + currentSequence);

                                // Send ACK for the received packet
                                sendAck(responsePacket, channel, routerAddress, timer, ackTasksMap);

                                String responsePayload = new String(responsePacket.getPayload(), StandardCharsets.UTF_8);

                                if (responsePayload.equalsIgnoreCase("\r\n"))
                                    finalPacket = responsePacket;
                                else {
                                    // Add packet to buffer
                                    receivedPackets.add(responsePacket);
                                }
                                sequenceNumbers.add(responsePacket.getSequenceNumber());
                            } else if (responsePacket.getType() == 0) {
                                sendAck(responsePacket, channel, routerAddress, timer, ackTasksMap);
                                System.out.println("Resensding ACK for Packet: " + responsePacket.getSequenceNumber());
                                continue;
                            }

                            start = (int) currentSequence;
                            ++Client.start;

                            System.out.println("Received Response List Size:" + receivedPackets.size());
                            System.out.println("Received Response Payload size:" + responsePacket.getPayload().length);

                            if (finalPacket != null && sequenceNumbers.size() == (finalPacket.getSequenceNumber() + 1)) {
                                flag = false;
                            }
                        }

                        System.out.println("In channel helper");
                        ChannelHelper channelHelper = new ChannelHelper(channel, routerAddress);
                        channelHelper.start();

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

                        channelHelper.join();

                        if (httpResponse.processResponse(response, requestParameters)) {
                            requestParameters.requestLine = requestParameters.redirectionUrl;
                            redirect = true;
                            System.out.println("\nREDIRECTING...\n");
                        }

                        packetList.clear();
                        ackBuffer.clear();
                        receivedPackets.clear();
                        finalPacket = null;
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.out.println("Your input is invalid, please try again!");
                }
            }
        }
    }

    private static void invokeThreads(RequestParameters requestParameters) {
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
        byte[] bytes;
        bytes = "\r\n".getBytes();
        for (int i = 0; i < buffer.length; i = i + 1013) {
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
        }

        Packet lastPacket = new Packet.Builder()
                .setPeerAddress(serverAddress.getAddress())
                .setPortNumber(serverAddress.getPort())
                .setSequenceNumber(modifySequence(true))
                .setType(0)
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
                timer.schedule(task, 3000, 5000);
                packetList.remove((long) i);
                Client.start++;
                return false;
            }

            channel.send(packetList.get((long) i).toBuffer(), routerAddress);
            TimerTask task = new PacketTimeout(packetList.get((long) i), channel, routerAddress);
            tasks.put((long) i, task);
            timer.schedule(task, 3000, 5000);
            packetList.remove((long) i);
            Client.start++;
        }

        return true;
    }

    private static void sendAck(Packet responsePacket, DatagramChannel channel, SocketAddress routerAddress, Timer timer, HashMap<Long, TimerTask> ackTasksMap) throws IOException {
        Packet packet = responsePacket.toBuilder()
                .setType(2)
                .setPayload(new byte[0])
                .create();

        channel.send(packet.toBuffer(), routerAddress);
        System.out.println("Sending ACK for packet: " + packet.getSequenceNumber());
    }

    protected static long modifySequence(boolean modify) {
        if (modify)
            currentSequence++;

        return currentSequence;
    }

    private static Packet connect(RequestParameters requestParameters) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            InetSocketAddress serverAddress = new InetSocketAddress(requestParameters.host, requestParameters.port);
            SocketAddress routerAddress = new InetSocketAddress(requestParameters.routerHost, requestParameters.routerPort);

            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);

            Set<SelectionKey> keys = selector.selectedKeys();

            Packet packet = new Packet.Builder()
                    .setType(1)
                    .setSequenceNumber(currentSequence)
                    .setPeerAddress(serverAddress.getAddress())
                    .setPortNumber(serverAddress.getPort())
                    .setPayload(new byte[200])
                    .create();

            sequenceNumbers.add(packet.getSequenceNumber());

            boolean flag = true;

            while (flag) {
                channel.send(packet.toBuffer(), routerAddress);

                selector.select(3000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();

                // Create Buffer for Response
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);

                // Receive Response
                channel.receive(buf);
                buf.flip();

                if (buf.limit() < Packet.MIN_LEN || buf.limit() > Packet.MAX_LEN)
                    continue;

                Packet responsePacket = Packet.fromBuffer(buf);
                buf.clear();

                if (responsePacket.getType() == 1 && !sequenceNumbers.contains(responsePacket.getSequenceNumber())) {
                    modifySequence(true);
                    sequenceNumbers.add(responsePacket.getSequenceNumber());
                    flag = false;
                }

                return responsePacket;
            }

            System.out.println("Handshake completed");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
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
