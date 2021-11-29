package log4j.layout.bunyan;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Scanner;

public class NaughtyStrings implements Iterable<String>, AutoCloseable {
    private final Scanner scanner;

    public NaughtyStrings() throws IOException {
        final Path path = Paths.get("src/test/resources/blns.txt");
        this.scanner = new Scanner(path);
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return scanner.hasNextLine();
            }

            @Override
            public String next() {
                final String line = scanner.nextLine();

                if (line.startsWith("#") && hasNext()) {
                    return next();
                }

                return line;
            }
        };
    }

    @Override
    public void close() {
        scanner.close();
    }
}
