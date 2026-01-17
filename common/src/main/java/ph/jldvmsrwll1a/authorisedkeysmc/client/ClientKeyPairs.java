package ph.jldvmsrwll1a.authorisedkeysmc.client;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ClientKeyPairs {
    public ClientKeyPairs() {
        try {
            Ed25519PrivateKeyParameters ignored = getDefaultKey();
        } catch (IOException ignored) {
            generate("default");
        }
    }

    public Ed25519PrivateKeyParameters getDefaultKey() throws IOException {
        return loadFromFile("default");
    }

    private Ed25519PrivateKeyParameters loadFromFile(String name) throws IOException {
        Path path = fromKeyName(name);
        byte[] keyBytes = Files.readAllBytes(path);
        PrivateKeyInfo pkcs = PrivateKeyInfo.getInstance(keyBytes);

        return (Ed25519PrivateKeyParameters) PrivateKeyFactory.createKey(pkcs);
    }

    private void generate(String name) {
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

    private Path fromKeyName(String name) {
        return AuthorisedKeysModCore.FILE_PATHS.KEY_PAIRS_DIR.resolve("%s.der".formatted(name));
    }
}
