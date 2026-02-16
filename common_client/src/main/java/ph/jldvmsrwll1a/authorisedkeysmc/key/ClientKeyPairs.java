package ph.jldvmsrwll1a.authorisedkeysmc.key;

import java.io.IOException;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;
import ph.jldvmsrwll1a.authorisedkeysmc.util.ValidPath;

public class ClientKeyPairs {
    public ClientKeyPairs() {
        ensureWarningFileExists();
    }

    public static Path fromKeyName(@NotNull String name) throws InvalidPathException {
        return ValidPath.makePath(AkmcCore.FILE_PATHS.KEY_PAIRS_DIR, name, Constants.KEY_PAIR_EXTENSION);
    }

    public List<String> retrieveKeyNamesFromDisk() {
        List<String> names = new ArrayList<>();

        try (DirectoryStream<Path> dirEntries =
                Files.newDirectoryStream(AkmcCore.FILE_PATHS.KEY_PAIRS_DIR)) {
            for (Path path : dirEntries) {
                String name = path.getFileName().toString();

                if (name.endsWith(Constants.KEY_PAIR_EXTENSION) && Files.isRegularFile(path)) {
                    names.add(name.substring(0, name.length() - Constants.KEY_PAIR_EXTENSION.length()));
                }
            }
        } catch (IOException e) {
            Constants.LOG.error("Could not iterate through directory: {}", e.getMessage());
        }

        return names;
    }

    public @NotNull AkKeyPair loadFromFile(@NotNull String name) throws IOException, InvalidPathException {
        return AkKeyPair.fromFile(fromKeyName(name), name);
    }

    public void generate(@NotNull String name, char @Nullable [] password) throws InvalidPathException {
        Path path = fromKeyName(name);

        try {
            AkKeyPair generated = AkKeyPair.generate(SecureRandom.getInstanceStrong(), name);

            if (password != null) {
                generated.encrypt(password);
            }

            generated.writeFile(path);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.info("Generated new keypair \"{}\".", name);
    }

    public void deleteKeyFile(@NotNull AkKeyPair keypair) {
        deleteKeyFile(keypair.getName());
    }

    public void deleteKeyFile(@NotNull String name) {
        try {
            Path path = fromKeyName(name);
            Validate.isTrue(Files.isRegularFile(path), "Refusing to delete anything but a regular file!");

            Files.deleteIfExists(path);
        } catch (IOException e) {
            Constants.LOG.warn("Failed to delete file for key {}: {}", name, e);
        }
    }

    private void ensureWarningFileExists() {
        final Path path = AkmcCore.FILE_PATHS.KEY_PAIRS_DIR.resolve("_SECRET_KEYS_DO_NOT_SHARE");

        try {
            Files.createDirectories(AkmcCore.FILE_PATHS.KEY_PAIRS_DIR);
            Files.createFile(path);
        } catch (FileAlreadyExistsException ignored) {
            // Do nothing.
        } catch (IOException e) {
            Constants.LOG.warn("Could not create warning file: {}", e.toString());
        }
    }
}
