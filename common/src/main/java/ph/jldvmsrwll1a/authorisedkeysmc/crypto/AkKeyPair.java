package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.util.Arrays;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

/**
 * Keypair that may or may not be encrypted.
 */
public class AkKeyPair {
    private final @NonNull String name;
    private Instant modificationTime;

    private @Nullable AkPublicKey publicKey;
    private @Nullable AkPrivateKey privateKey;
    private @Nullable AkEncryptedKey encryptedKey;

    public AkKeyPair(
            @NonNull String name,
            @NonNull Instant modificationTime,
            @NonNull AkPrivateKey privateKey,
            @Nullable AkPublicKey publicKey) {
        this.name = name;
        this.modificationTime = modificationTime;

        this.privateKey = privateKey;
        this.publicKey = publicKey != null ? publicKey : privateKey.derivePublicKey();
        this.encryptedKey = null;
    }

    public AkKeyPair(
            @NonNull String name,
            @NonNull Instant modificationTime,
            @NonNull AkEncryptedKey encryptedKey,
            @Nullable AkPublicKey publicKey) {
        this.name = name;
        this.modificationTime = modificationTime;

        this.privateKey = null;
        this.publicKey = publicKey;
        this.encryptedKey = encryptedKey;
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
        } else if (encryptedKey != null) {
            throw new IllegalStateException("private key has likely not yet been decrypted");
        } else {
            throw new IllegalStateException("Degenerate keypair does not contain any data.");
        }
    }

    /**
     * Does this keypair need to be decrypted in order to be usable?
     */
    public boolean requiresDecryption() {
        return privateKey == null && encryptedKey != null;
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
        Validate.validState(encryptedKey != null, "Not an encrypted private key.");

        try {
            encryptedKey.decrypt(password).ifPresent(this::setPrivateKey);
        } finally {
            Arrays.fill(password, '\0');
        }

        return !requiresDecryption();
    }

    /**
     * Encrypt the keypair in-place with the provided password. Does not erase the unencrypted private key.
     * @param password The password to encrypt with.
     */
    public void encrypt(char @NonNull [] password) {
        Validate.validState(privateKey != null, "Decrypted private key not available.");

        try {
            encryptedKey = new AkEncryptedKey(SecureRandom.getInstanceStrong(), privateKey, password);
        } catch (NoSuchAlgorithmException e) {
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
     * Write key pair information to a file on {@code outPath}.
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

        ByteBuffer outBuffer = ByteBuffer.allocate(4096);
        outBuffer.putInt(Constants.KEY_PAIR_HEADER);
        outBuffer.putShort(Constants.KEY_PAIR_VERSION);

        if (encryptedKey != null && publicKey != null) {
            outBuffer.putShort((short) 1);
            publicKey.write(outBuffer);
            encryptedKey.write(outBuffer);
        } else if (privateKey != null) {
            outBuffer.putShort((short) 0);
            privateKey.write(outBuffer);
        } else {
            throw new IllegalStateException("Degenerate keypair has no private key");
        }

        try (FileChannel channel = FileChannel.open(outPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            outBuffer.flip();

            while (outBuffer.hasRemaining()) {
                int len = channel.write(outBuffer);
            }
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
     * Loads a key pair from a file.
     * @param name A label for the key.
     * @return The loaded keypair.
     * @throws IOException May fail to read the file.
     */
    public static AkKeyPair fromFile(@NonNull Path path, @NonNull String name) throws IOException {
        Instant modificationTime = Files.getLastModifiedTime(path).toInstant();

        ByteBuffer inBuffer = ByteBuffer.allocate(4096);

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            while (true) {
                if (channel.read(inBuffer) < 0)
                    break;
            }

            inBuffer.flip();
        }

        int header = inBuffer.getInt();
        Validate.isTrue(header == Constants.KEY_PAIR_HEADER, "Header mismatch: expected 0x%x, got 0x%x.", Constants.KEY_PAIR_HEADER, header);

        short version = inBuffer.getShort();
        Validate.isTrue(version == Constants.KEY_PAIR_VERSION, "Version mismatch: expected %s, got %s.", Constants.KEY_PAIR_VERSION, version);

        short flags = inBuffer.getShort();

        if (flags == 1) {
            AkPublicKey publicKey = new AkPublicKey(inBuffer);
            AkEncryptedKey encryptedKey = new AkEncryptedKey(inBuffer);

            return new AkKeyPair(name, modificationTime, encryptedKey, publicKey);
        } else if (flags == 0) {
            AkPrivateKey privateKey = new AkPrivateKey(inBuffer);

            return new AkKeyPair(name, modificationTime, privateKey, null);
        } else {
            throw new IllegalArgumentException("Unknown flags value: %x".formatted(flags));
        }
    }
}
