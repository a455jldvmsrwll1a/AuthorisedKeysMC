package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.Instant;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfoBuilder;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Keypair that may or may not be encrypted.
 */
public class AkKeyPair {
    private final @NonNull String name;
    private Instant modificationTime;

    private @Nullable AkPublicKey publicKey;
    private @Nullable AkPrivateKey privateKey;
    private @Nullable PKCS8EncryptedPrivateKeyInfo encryptedInfo;

    public AkKeyPair(
            @NonNull String name,
            @NonNull Instant modificationTime,
            @NonNull AkPrivateKey privateKey,
            @Nullable AkPublicKey publicKey) {
        this.name = name;
        this.modificationTime = modificationTime;

        this.privateKey = privateKey;
        this.publicKey = publicKey != null ? publicKey : privateKey.derivePublicKey();
        this.encryptedInfo = null;
    }

    public AkKeyPair(
            @NonNull String name,
            @NonNull Instant modificationTime,
            @NonNull PKCS8EncryptedPrivateKeyInfo encryptedInfo,
            @Nullable AkPublicKey publicKey) {
        this.name = name;
        this.modificationTime = modificationTime;

        this.privateKey = null;
        this.publicKey = publicKey;
        this.encryptedInfo = encryptedInfo;
    }

    /**
     * Get the name of this keypair.
     */
    public @NonNull String getName() {
        return name;
    }

    /**
     * Get the latest modification time of this keypair.
     */
    public @NonNull Instant getModificationTime() {
        return modificationTime;
    }

    /**
     * Get a textual representation of the public key. This is intended to define the "canonical" format that will be
     * used by users and stored in files.
     */
    public @NonNull String getTextualPublic() {
        return getPublic().toString();
    }

    /**
     * Get the public key of this keypair. The keypair's private key must already be unencrypted or this will throw.
     */
    public @NonNull AkPublicKey getPublic() {
        if (publicKey != null) {
            return publicKey;
        } else {
            throw new IllegalStateException(
                    "public key is undetermined (private key has likely not yet been decrypted, and no public key was explicitly supplied in the key file)");
        }
    }

    /**
     * Get the private key of this keypair. The keypair must already be decrypted or this will throw.
     */
    public @NonNull AkPrivateKey getDecryptedPrivate() {
        if (privateKey != null) {
            return privateKey;
        } else if (encryptedInfo != null) {
            throw new IllegalStateException("private key has likely not yet been decrypted");
        } else {
            throw new IllegalStateException("Degenerate keypair does not contain any data.");
        }
    }

    /**
     * Does this keypair need to be decrypted in order to be usable?
     */
    public boolean requiresDecryption() {
        return privateKey == null && encryptedInfo != null;
    }

    /**
     * Manually set the private key of this keypair. If present, the public key must correspond with the private key.
     * <p>
     * Care should be taken if the new private key is different from the current encrypted private key.
     * @param secret The new private key.
     */
    public void setPrivateKey(@NonNull AkPrivateKey secret) {
        AkPublicKey newPublicKey = secret.derivePublicKey();

        if (publicKey != null && !publicKey.equals(newPublicKey)) {
            throw new IllegalStateException("Provided private key does not match the current public key.");
        }

        privateKey = secret;
        if (publicKey == null) {
            publicKey = newPublicKey;
        }
        modificationTime = Instant.now();
    }

    /**
     * Attempt to decrypt the keypair in-place with the provided password. Never erases the encrypted private key.
     * @param password The password to decrypt with.
     * @return {@code true} if successful.
     */
    public boolean decrypt(char @NonNull [] password) {
        Validate.validState(encryptedInfo != null, "Not an encrypted private key.");

        try {
            InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password);
            PrivateKeyInfo pki = encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
            AsymmetricKeyParameter key = PrivateKeyFactory.createKey(pki);

            if (key instanceof Ed25519PrivateKeyParameters edPri) {
                setPrivateKey(new AkPrivateKey(edPri.getEncoded()));

                return true;
            } else {
                throw new IllegalArgumentException("Expected a %s but found a %s!"
                        .formatted(
                                Ed25519PublicKeyParameters.class.getName(),
                                key.getClass().getName()));
            }
        } catch (OperatorCreationException | IOException e) {
            throw new RuntimeException(e);
        } catch (PKCSException e) {
            // Nothing.
        } finally {
            Arrays.fill(password, '\0');
        }

        return false;
    }

    /**
     * Encrypt the keypair in-place with the provided password. Does not erase the unencrypted private key.
     * @param password The password to encrypt with.
     */
    public void encrypt(char @NonNull [] password) {
        Validate.validState(privateKey != null, "Decrypted private key not available.");

        try {
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(privateKey.getInternal());

            JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder =
                    new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC);
            encryptorBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);
            encryptorBuilder.setRandom(new SecureRandom());
            encryptorBuilder.setIterationCount(2_000_000);
            encryptorBuilder.setPassword(password);
            OutputEncryptor encryptor = encryptorBuilder.build();

            encryptedInfo = new PKCS8EncryptedPrivateKeyInfoBuilder(privateKeyInfo).build(encryptor);
        } catch (OperatorCreationException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            Arrays.fill(password, '\0');
        }

        modificationTime = Instant.now();
    }

    @Override
    public String toString() {
        return getTextualPublic();
    }

    /**
     * Emit a PEM file containing both the private key and the public key.
     * <p>
     * An {@code ENCRYPTED PRIVATE KEY} PEM object will be emitted if the keypair contains an encrypted private key.
     * Otherwise, it will emit a {@code PRIVATE KEY} PEM object instead.
     * @param outPath The file path to write to.
     * @throws IOException May fail to write the file.
     */
    public void writeFile(@NonNull Path outPath) throws IOException {
        Path path = outPath.normalize();
        Files.createDirectories(path.getParent());

        try {
            Path backup = Path.of("%s.BACKUP".formatted(path));
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Don't care.
        }

        PemObject privatePem;
        if (encryptedInfo != null) {
            privatePem = new PemObject("ENCRYPTED PRIVATE KEY", encryptedInfo.getEncoded());
        } else if (privateKey != null) {
            PrivateKeyInfo pki = PrivateKeyInfoFactory.createPrivateKeyInfo(privateKey.getInternal());
            privatePem = new PemObject("PRIVATE KEY", pki.getEncoded());
        } else {
            throw new IllegalStateException("Degenerate keypair has no private key");
        }

        SubjectPublicKeyInfo spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(
                getPublic().getInternal());
        PemObject publicPem = new PemObject("PUBLIC KEY", spki.getEncoded());

        try (PemWriter writer = new PemWriter(new FileWriter(path.toFile()))) {
            writer.writeObject(privatePem);
            writer.writeObject(publicPem);
        }
    }

    /**
     * Generate a new keypair.
     * @param random RNG to use for the generator.
     * @param name A label for the key.
     * @return The newly created keypair.
     */
    public static @NonNull AkKeyPair generate(@NonNull SecureRandom random, @NonNull String name) {
        AkPrivateKey privateKey = new AkPrivateKey(random);
        AkPublicKey publicKey = privateKey.derivePublicKey();

        Instant now = Instant.now();

        return new AkKeyPair(name, now, privateKey, publicKey);
    }

    /**
     * Loads a keypair from a PEM file.
     * @param name A label for the key.
     * @return The loaded keypair.
     * @throws IOException May fail to read the file.
     */
    public static AkKeyPair fromFile(@NonNull Path path, @NonNull String name) throws IOException {
        Instant modificationTime = Files.getLastModifiedTime(path).toInstant();

        AkPrivateKey privateKey = null;
        AkPublicKey publicKey = null;
        PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = null;

        try (PemReader reader = new PemReader(new FileReader(path.toFile()))) {
            while (true) {
                PemObject pem = reader.readPemObject();

                if (pem == null) {
                    break;
                }

                switch (pem.getType()) {
                    case "PRIVATE KEY" -> {
                        AsymmetricKeyParameter key = PrivateKeyFactory.createKey(pem.getContent());

                        if (key instanceof Ed25519PrivateKeyParameters edPri) {
                            privateKey = new AkPrivateKey(edPri.getEncoded());
                        } else {
                            throw new IllegalArgumentException("expected a private key of type %s but found a %s"
                                    .formatted(
                                            Ed25519PrivateKeyParameters.class.getName(),
                                            key.getClass().getName()));
                        }
                    }
                    case "ENCRYPTED PRIVATE KEY" ->
                        encryptedPrivateKeyInfo = new PKCS8EncryptedPrivateKeyInfo(pem.getContent());
                    case "PUBLIC KEY" -> {
                        AsymmetricKeyParameter key = PublicKeyFactory.createKey(pem.getContent());

                        if (key instanceof Ed25519PublicKeyParameters edPub) {
                            publicKey = new AkPublicKey(edPub.getEncoded());
                        } else {
                            throw new IllegalArgumentException("expected a public key of type %s but found a %s"
                                    .formatted(
                                            Ed25519PublicKeyParameters.class.getName(),
                                            key.getClass().getName()));
                        }
                    }
                }
            }
        }

        if (privateKey != null) {
            return new AkKeyPair(name, modificationTime, privateKey, publicKey);
        } else if (encryptedPrivateKeyInfo != null) {
            return new AkKeyPair(name, modificationTime, encryptedPrivateKeyInfo, publicKey);
        } else {
            throw new IllegalArgumentException("File contains no valid data.");
        }
    }
}
