package ph.jldvmsrwll1a.authorisedkeysmc.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class WriteUtil {
    private WriteUtil() {}

    public static void writeString(Path path, CharSequence content) throws IOException {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");

        try {
            Files.writeString(
                    temp, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(temp);

            throw e;
        }
    }
}
