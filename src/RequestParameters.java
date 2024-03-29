import java.util.ArrayList;

public class RequestParameters {
    boolean isVerbose;
    boolean isInline;
    boolean readFromFile;
    boolean writeToFile;
    boolean hasHeaders;
    boolean isGetRequest;
    boolean isPostRequest;
    boolean isValid;

    int indexVerbose;
    int indexInline;
    int indexWriteToFile;
    int port;
    int routerPort;

    ArrayList<String> headers;

    String host;
    String requestLine;
    String data;
    String inputFile;
    String outputFile;
    String redirectionUrl;
    String routerHost;

    public RequestParameters() {
        this.indexVerbose = -1;
        this.indexInline = -1;
        this.indexWriteToFile = -1;
        this.port = 8080;
        this.routerPort = 3000;
        this.headers = new ArrayList<>();
        this.host = "";
        this.requestLine = "";
        this.data = "";
        this.inputFile = "";
        this.outputFile = "";
        this.redirectionUrl = "";
        this.routerHost = "localhost";
    }
}
