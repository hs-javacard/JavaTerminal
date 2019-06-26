package terminal;

import java.io.File;
import java.io.FileOutputStream;
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
        File file = new File(System.getProperty("user.home") + File.separator + path);

        if (!file.exists()) {
            boolean success = file.createNewFile();
            if (!success)
                throw new IOException("File could not be created");
        }
        PrintWriter writer = new PrintWriter(new FileOutputStream(file, true));
        writer.append(content);
        writer.append("\n");
        writer.flush();
        writer.close();
        return file;

    }
}
