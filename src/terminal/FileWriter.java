package terminal;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public final class FileWriter {

    private FileWriter() {
        //Abstract class cannot be initialized.
    }

    /**
     * Write the given contents of to a file located at the given path.
     *
     * @param path to the file.
     * @return a file containing the given contents.
     * @throws IOException when something is wrong with the given path.
     */
    public static File write(String content, String path) throws IOException {
        File file = new File(path);

        boolean success = file.createNewFile();
        if (success)
            throw new IOException("File could not be created");

        PrintWriter writer = new PrintWriter(file);
        writer.print(content);
        writer.flush();
        writer.close();
        return file;

    }
}
