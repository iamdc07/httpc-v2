import java.net.MalformedURLException;
import java.net.URL;

public class HttpRequest {
    public String processRequest(RequestParameters requestParameters) {
        String payload = "";

        try {
            if (requestParameters.readFromFile) {
                FileOperations fileOperations = new FileOperations();
                fileOperations.readFile(requestParameters);
            }

            if (requestParameters.isGetRequest)
                payload = getRequest(requestParameters);
            else
                payload = postRequest(requestParameters);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }

        return payload;
    }

    @SuppressWarnings("DuplicatedCode")
    private String getRequest(RequestParameters requestParameters) {
        String request = "";
        try {
            URL url = new URL(requestParameters.requestLine);
            requestParameters.host = url.getHost();

            request = request.concat("GET " + url.getFile() + " HTTP/1.0\r\n");
            request = request.concat("Host: " + requestParameters.host + "\r\n");
            request = request.concat("User-Agent: " + "httpc client" + "\r\n");

            if (requestParameters.hasHeaders) {
                for (String header : requestParameters.headers) {
                    String[] data = header.split(":");
                    request = request.concat(data[0] + ":" + " " + data[1] + "\r\n");
                }
            }
            request = request.concat("\r\n");
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
        return request;
    }

    @SuppressWarnings("DuplicatedCode")
    private String postRequest(RequestParameters requestParameters) throws MalformedURLException {
        URL url = new URL(requestParameters.requestLine);
        requestParameters.host = url.getHost();

        String request = "";

        request = request.concat("POST " + url.getPath() + " HTTP/1.1\r\n");
        request = request.concat("Content-Length: " + requestParameters.data.length() + "\r\n");
        request = request.concat("Connection: " + "close" + "\r\n");
        request = request.concat("Host: " + requestParameters.host + "\r\n");

        if (requestParameters.hasHeaders) {
            for (String header : requestParameters.headers) {
                String[] data = header.split(":");
                request = request.concat(data[0] + ":" + " " + data[1] + "\r\n");
            }
        }

        request = request.concat("\r\n");
        request = request.concat(requestParameters.data);

        return request;
    }
}

//httpc post -v -f input.txt http://localhost/test
//httpc post -v -d network http://localhost:8080/foo
//httpc post -v -f input.txt http://localhost:8080/temp
//httpc get -v -h Content-Type:application/json http://localhost/sample
//httpc get -v -h Content-Type:application/text http://localhost/sample
//httpc get -v -h Content-Disposition:attachment http://localhost/Server.java
//httpc get -v -h Content-Disposition:inline http://localhost/Server.java
//httpc post -v -h Content-Type:application/plain --d NetworkBook http://httpbin.org/post
//httpc post -d NetworkBook http://httpbin.org/post
//httpc post -v -h Content-Type:application/json -d {"Assignment":1} http://httpbin.org/post
//httpc post -v -f input.txt http://httpbin.org/post
//httpc post -v -f input.txt --d Networks http://httpbin.org/post -o hello.txt
//httpc post -v --d Netowrks http://httpbin.org/post -o hello.txt
//httpc -v --d Netowrks http://httpbin.org/post -o hello.txt
//httpc get -v http://google.com/
//httpc get -v http://www.google.com/
//httpc get -v -h Content-Type:application/plain http://httpbin.org/status/418
//httpc get -v -h Content-Type:application/plain -h Name:Yashas http://httpbin.org/get?course=networking&assignment=1 -o hello.txt