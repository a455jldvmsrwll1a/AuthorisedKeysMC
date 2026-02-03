package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfoBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.util.ValidPath;

public class ClientKeyPairs {
    public ClientKeyPairs() {
        ensureWarningFileExists();

        if (retrieveKeyNamesFromDisk().isEmpty()) {
            generate("default", null);
        }
    }

    public static Path fromKeyName(@NotNull String name) throws InvalidPathException {
        return ValidPath.makePath(AuthorisedKeysModCore.FILE_PATHS.KEY_PAIRS_DIR, name, ".pem");
    }

    public List<String> retrieveKeyNamesFromDisk() {
        List<String> names = new ArrayList<>();

        try (DirectoryStream<Path> dirEntries =
                Files.newDirectoryStream(AuthorisedKeysModCore.FILE_PATHS.KEY_PAIRS_DIR)) {
            for (Path path : dirEntries) {
                String name = path.getFileName().toString();

                if (name.endsWith(".pem") && Files.isRegularFile(path)) {
                    names.add(name.substring(0, name.length() - 4));
                }
            }
        } catch (IOException e) {
            Constants.LOG.error("Could not iterate through directory: {}", e.getMessage());
        }

        return names;
    }

    public @NotNull LoadedKeypair loadFromFile(@NotNull String name) throws IOException, InvalidPathException {
        return LoadedKeypair.fromFile(fromKeyName(name), name);
    }

    public void generate(@NotNull String name, char @Nullable [] password) throws InvalidPathException {
        Path path = fromKeyName(name);
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
            Ed25519PrivateKeyParameters privateKey = (Ed25519PrivateKeyParameters) kp.getPrivate();
            Ed25519PublicKeyParameters publicKey = (Ed25519PublicKeyParameters) kp.getPublic();

            PrivateKeyInfo privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(privateKey);
            SubjectPublicKeyInfo pubInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey);

            PemObject privatePem;

            if (password != null) {
                JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder =
                        new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC);
                encryptorBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);
                encryptorBuilder.setRandom(new SecureRandom());
                encryptorBuilder.setIterationCount(2_000_000);
                encryptorBuilder.setPassword(password);
                OutputEncryptor encryptor = encryptorBuilder.build();

                PKCS8EncryptedPrivateKeyInfo encryptedInfo =
                        new PKCS8EncryptedPrivateKeyInfoBuilder(privateKeyInfo).build(encryptor);

                privatePem = new PemObject("ENCRYPTED PRIVATE KEY", encryptedInfo.getEncoded());
            } else {
                privatePem = new PemObject("PRIVATE KEY", privateKeyInfo.getEncoded());
            }

            PemObject publicPem = new PemObject("PUBLIC KEY", pubInfo.getEncoded());

            try (PemWriter writer = new PemWriter(new FileWriter(path.toFile()))) {
                writer.writeObject(privatePem);
                writer.writeObject(publicPem);
            }
        } catch (IOException | NoSuchAlgorithmException | OperatorCreationException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.info("Generated new keypair \"{}\".", name);
    }

    public void deleteKeyFile(@NotNull LoadedKeypair keypair) {
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
}
