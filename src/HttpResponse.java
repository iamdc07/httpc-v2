import java.io.IOException;

public class HttpResponse {
    public boolean processResponse(String response, RequestParameters requestParameters) throws IOException {
        int index = -1;

        if (requestParameters.isVerbose) {
            System.out.print(response);
        } else if (!processContentDisposition(response, requestParameters)){
            index = response.indexOf("\n\r");
            for (int i = index + 2; i < response.length(); i++) {
                System.out.print(response.charAt(i));
            }
            System.out.println();
        }


        if (requestParameters.writeToFile)
            new FileOperations().writeFile(requestParameters, response);

        if (response.contains("301 Moved")) {
            index = response.indexOf("Location:") + 10;
            for (int i = index; i < response.length(); i++) {
                if (response.charAt(i) == '\r')
                    break;

                requestParameters.redirectionUrl = requestParameters.redirectionUrl.concat(String.valueOf(response.charAt(i)));
            }
            return true;
        } else
            return false;

    }

    public boolean processContentDisposition(String response, RequestParameters requestParameters) throws IOException {
        FileOperations fileOperations = new FileOperations();
        String temp = requestParameters.outputFile;
        String data = "";
        boolean flag = false;

        String[] lines = response.split("\r\n");

        for (int i = 0; i < lines.length; i++) {
            String[] words = lines[i].split(" ");

            if (words[0].equalsIgnoreCase("Content-Disposition:")) {
                flag = true;
                if (words[1].equalsIgnoreCase("attachment;")) {
                    int index = response.indexOf("\n\r");
                    for (int j = index + 2; j < response.length(); j++) {
                        data = data.concat(String.valueOf(response.charAt(j)));
                    }
                    System.out.println();

                    if (lines[i].contains("filename")) {
                        requestParameters.outputFile = words[words.length - 1];
                        fileOperations.writeFile(requestParameters, data);
                    } else {
                        requestParameters.outputFile = "NewFile1.txt";
                        fileOperations.writeFile(requestParameters, data);
                    }
                    requestParameters.outputFile = temp;
                }
            }
        }
        return flag;
    }
}
