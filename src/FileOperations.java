import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileOperations {
    public void readFile(RequestParameters requestParameters) {
        try {
            String filePath = "../../../files/".concat(requestParameters.inputFile);
            Path path = Paths.get(filePath).normalize().toAbsolutePath();
            Path resolvedPath = path.resolve(path).normalize().toAbsolutePath();

            requestParameters.data = new String(Files.readAllBytes(resolvedPath));
        } catch (IOException ex) {
            System.out.println("Something went wrong!");
            ex.printStackTrace();
        }
    }

    public void writeFile(RequestParameters requestParameters, String response) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter("../../../files/" + requestParameters.outputFile));
        out.write(response);
        out.flush();
        out.close();
    }
}