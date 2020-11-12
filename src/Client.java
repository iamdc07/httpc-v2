import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws IOException, InterruptedException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        HttpResponse httpResponse = new HttpResponse();
        Charset utf8 = StandardCharsets.UTF_8;
        RequestParameters requestParameters = null;
        String response = "", input = "", choice = "";
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
//                    RequestParameters requestParameters1 = new RequestParameters();
//                    requestParameters1.isVerbose = true;
//                    requestParameters1.isInline = true;
//                    requestParameters1.isPostRequest = true;
//                    requestParameters1.isValid = true;
//                    requestParameters1.indexVerbose = 11;
//                    requestParameters1.requestLine = "http://localhost/foo";
//                    requestParameters1.data = "Replacing the existing data";
//                    invokeThreads(requestParameters1);
                    continue;
                }

                if (requestParameters.isValid) {
                    HttpRequest httpRequest = new HttpRequest();
                    String payload = httpRequest.processRequest(requestParameters);
                    ByteBuffer byteBuffer = utf8.encode(payload);

                    try {
                        SocketAddress socketAddress = new InetSocketAddress(requestParameters.host, requestParameters.port);
                        SocketChannel socketChannel = SocketChannel.open();
                        socketChannel.connect(socketAddress);
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
                        socketChannel.close();

                        if (httpResponse.processResponse(response, requestParameters)) {
                            requestParameters.requestLine = requestParameters.redirectionUrl;
                            redirect = true;
                            System.out.println("\nREDIRECTING...\n");
                        }
                        response = "";
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.out.println("Your input is invalid, please try again!");
                }
            }
        }
    }

    private static void invokeThreads(RequestParameters requestParameters) throws InterruptedException, IOException {
        SocketAddress socketAddress = new InetSocketAddress(requestParameters.host, 8080);

        for (int i = 0; i < 200; i++) {
            HandleThreads handleThreads = new HandleThreads(requestParameters, socketAddress);
            handleThreads.start();
        }

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
