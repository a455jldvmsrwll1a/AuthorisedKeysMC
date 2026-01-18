package ph.jldvmsrwll1a.authorisedkeysmc;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;

public class ServerKeypair {
    public Ed25519PublicKeyParameters publicKey;
    public Ed25519PrivateKeyParameters secretKey;

    public ServerKeypair() {
        try {
            loadFromFile();
        } catch (Exception ignored) {
            generate();
        }
    }

    private void loadFromFile() {
        try {
            byte[] secretBytes = Files.readAllBytes(AuthorisedKeysModCore.FILE_PATHS.SERVER_SECRET_PATH);
            PrivateKeyInfo info = PrivateKeyInfo.getInstance(secretBytes);
            secretKey = (Ed25519PrivateKeyParameters) PrivateKeyFactory.createKey(info);
            publicKey = secretKey.generatePublicKey();
        } catch (IOException e) {
            Constants.LOG.warn("Could not find existing server keypair.");
            throw new RuntimeException(e);
        }

        Constants.LOG.info("Loaded existing server keypair.");
    }

    private void generate() {
        Path secret_path = AuthorisedKeysModCore.FILE_PATHS.SERVER_SECRET_PATH;
        Path backup_path = AuthorisedKeysModCore.FILE_PATHS.SERVER_SECRET_BACKUP_PATH;

        try {
            Files.createDirectories(AuthorisedKeysModCore.FILE_PATHS.MOD_DIR);

            try {
                if (Files.exists(secret_path)) {
                    Constants.LOG.warn("Backing up existing keypair file!");
                }

                Files.move(secret_path, backup_path);
            } catch (IOException ignored) {

            }

            secretKey = new Ed25519PrivateKeyParameters(SecureRandom.getInstanceStrong());
            publicKey = secretKey.generatePublicKey();
            PrivateKeyInfo pkcs = PrivateKeyInfoFactory.createPrivateKeyInfo(secretKey);

            Files.write(secret_path, pkcs.getEncoded());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.info("Generated new server keypair.");
    }
}
