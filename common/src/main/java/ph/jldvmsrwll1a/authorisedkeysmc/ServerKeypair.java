package ph.jldvmsrwll1a.authorisedkeysmc;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPrivateKey;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;

public class ServerKeypair {
    public AkPublicKey publicKey;
    public AkPrivateKey secretKey;

    public ServerKeypair() {
        try {
            loadFromFile();
        } catch (Exception ignored) {
            generate();
        }
    }

    private void loadFromFile() {
        Path path = AuthorisedKeysModCore.FILE_PATHS.SERVER_SECRET_PATH;

        try (PemReader reader = new PemReader(new FileReader(path.toFile()))) {
            while (true) {
                PemObject pem = reader.readPemObject();

                if (pem == null) {
                    break;
                }

                if (pem.getType().equals("PRIVATE KEY")) {
                    AsymmetricKeyParameter key = PrivateKeyFactory.createKey(pem.getContent());

                    if (key instanceof Ed25519PrivateKeyParameters edPri) {
                        secretKey = new AkPrivateKey(edPri.getEncoded());
                        publicKey = secretKey.derivePublicKey();

                        Constants.LOG.info("Loaded existing server keypair.");
                        return;
                    } else {
                        throw new IllegalArgumentException("Expected a %s but found a %s!"
                                .formatted(
                                        Ed25519PrivateKeyParameters.class.getName(),
                                        key.getClass().getName()));
                    }
                }
            }
        } catch (IOException e) {
            Constants.LOG.warn("Could not find existing server keypair.");
            throw new RuntimeException(e);
        }

        throw new IllegalArgumentException("File did not contain a valid private key.");
    }

    private void generate() {
        Path path = AuthorisedKeysModCore.FILE_PATHS.SERVER_SECRET_PATH;
        Path backupPath = Path.of("%s.BACKUP".formatted(path));

        try {
            try {
                Files.createDirectories(AuthorisedKeysModCore.FILE_PATHS.KEY_PAIRS_DIR);
            } catch (FileAlreadyExistsException ignored) {

            }

            try {
                if (Files.exists(backupPath)) {
                    Constants.LOG.warn("Backing up existing keypair file: {}", backupPath);
                }

                Files.move(path, backupPath);
            } catch (IOException ignored) {

            }

            Ed25519KeyPairGenerator generator = new Ed25519KeyPairGenerator();
            generator.init(new Ed25519KeyGenerationParameters(SecureRandom.getInstanceStrong()));

            AsymmetricCipherKeyPair kp = generator.generateKeyPair();
            secretKey = new AkPrivateKey(((Ed25519PrivateKeyParameters) kp.getPrivate()).getEncoded());
            publicKey = new AkPublicKey(((Ed25519PublicKeyParameters) kp.getPublic()).getEncoded());

            PrivateKeyInfo privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(kp.getPrivate());

            PemObject privatePem = new PemObject("PRIVATE KEY", privateKeyInfo.getEncoded());

            try (PemWriter writer = new PemWriter(new FileWriter(path.toFile()))) {
                writer.writeObject(privatePem);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.info("Generated new server keypair!");
    }
}
