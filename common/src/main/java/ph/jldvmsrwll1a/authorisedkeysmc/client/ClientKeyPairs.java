package ph.jldvmsrwll1a.authorisedkeysmc.client;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

import java.io.IOException;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ClientKeyPairs {
    public ClientKeyPairs() {
        ensureWarningFileExists();

        try {
            Ed25519PrivateKeyParameters ignored = loadFromFile("default");
        } catch (IOException ignored) {
            generate("default");
        }
    }

    public List<String> retrieveKeyNamesFromDisk() {
        List<String> names = new ArrayList<>();

        try (DirectoryStream<Path> dirEntries = Files.newDirectoryStream(AuthorisedKeysModCore.FILE_PATHS.KEY_PAIRS_DIR)) {
            for (Path path : dirEntries) {
                String name = path.getFileName().toString();

                if (name.endsWith(".der") && Files.isRegularFile(path)) {
                    names.add(name.substring(0, name.length() - 4));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return names;
    }

    public Instant getModificationTime(String name) throws IOException, InvalidPathException {
        Path path = fromKeyName(name);
        return Files.getLastModifiedTime(path).toInstant();
    }

    public Ed25519PrivateKeyParameters loadFromFile(String name) throws IOException, InvalidPathException {
        Path path = fromKeyName(name);
        byte[] keyBytes = Files.readAllBytes(path);
        PrivateKeyInfo pkcs = PrivateKeyInfo.getInstance(keyBytes);

        return (Ed25519PrivateKeyParameters) PrivateKeyFactory.createKey(pkcs);
    }

    private void generate(String name) throws InvalidPathException {
        Path path = fromKeyName(name);
        Path backupPath = Path.of("%s.BACKUP".formatted(path));

        try {
            Files.createDirectories(AuthorisedKeysModCore.FILE_PATHS.KEY_PAIRS_DIR);

            try {
                if (Files.exists(backupPath)) {
                    Constants.LOG.warn("Backing up existing keypair file: {}", backupPath);
                }

                Files.move(path, backupPath);
            } catch (IOException ignored) {

            }

            Ed25519PrivateKeyParameters secretKey = new Ed25519PrivateKeyParameters(SecureRandom.getInstanceStrong());
            PrivateKeyInfo pkcs = PrivateKeyInfoFactory.createPrivateKeyInfo(secretKey);
            Files.write(path, pkcs.getEncoded());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.info("Generated new keypair \"{}\".", name);
    }

    private void ensureWarningFileExists() {
        final Path path = AuthorisedKeysModCore.FILE_PATHS.KEY_PAIRS_DIR.resolve("_SECRET_KEYS_DO_NOT_SHARE");

        try {
            Files.createDirectories(AuthorisedKeysModCore.FILE_PATHS.KEY_PAIRS_DIR);
            Files.createFile(path);
        } catch (FileAlreadyExistsException ignored) {
            // Do nothing.
        } catch (IOException e) {
            Constants.LOG.warn("Could not create warning file: {}", e.toString());
        }
    }

    private Path fromKeyName(String name) throws InvalidPathException {
        return AuthorisedKeysModCore.FILE_PATHS.KEY_PAIRS_DIR.resolve("%s.der".formatted(name));
    }
}
