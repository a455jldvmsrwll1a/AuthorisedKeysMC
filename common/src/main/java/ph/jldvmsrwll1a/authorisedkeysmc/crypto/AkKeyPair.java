package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.NullMarked;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

/**
 * Keypair that may or may not be encrypted.
 */
@NullMarked
public abstract sealed class AkKeyPair permits AkKeyPair.Plain, AkKeyPair.Encrypted {
    // Only encrypted key pair files will have this size.
    // Unencrypted ones are a lot smaller.
    protected static final int MAX_FILE_SIZE = 136;

    protected final String name;
    protected final Instant modificationTime;
    protected final AkPublicKey publicKey;

    private AkKeyPair(String name, Instant modificationTime, AkPublicKey publicKey) {
        this.name = name;
        this.modificationTime = modificationTime;
        this.publicKey = publicKey;
    }

    /**
     * Get the name of this keypair.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the latest modification time of this keypair.
     */
    public Instant getModificationTime() {
        return modificationTime;
    }

    /**
     * Get a textual representation of the public key. This is intended to define the "canonical" format that will be
     * used by users and stored in files.
     */
    public String getTextualPublic() {
        return getPublic().toString();
    }

    public AkPublicKey getPublic() {
        return publicKey;
    }

    public boolean isEncrypted() {
        return this instanceof AkKeyPair.Encrypted;
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
    public void writeFile(Path outPath) throws IOException {
        Path path = outPath.normalize();
        Files.createDirectories(path.getParent());

        try {
            Path backup = Path.of("%s.BACKUP".formatted(path));
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Don't care.
        }

        ByteBuffer outBuffer = ByteBuffer.allocate(MAX_FILE_SIZE);
        outBuffer.putInt(Constants.KEY_PAIR_HEADER);
        outBuffer.putShort(Constants.KEY_PAIR_VERSION);

        writeSpecificData(outBuffer);

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
    public static AkKeyPair.Plain generate(SecureRandom random, String name) {
        AkPrivateKey privateKey = new AkPrivateKey(random);
        Instant now = Instant.now();

        return new AkKeyPair.Plain(name, now, privateKey);
    }

    /**
     * Loads a key pair from a file.
     * @param name A label for the key.
     * @return The loaded keypair.
     * @throws IOException May fail to read the file.
     */
    public static AkKeyPair fromFile(Path path, String name) throws IOException {
        Instant modificationTime = Files.getLastModifiedTime(path).toInstant();

        ByteBuffer inBuffer = ByteBuffer.allocate(MAX_FILE_SIZE);

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            if (channel.size() > MAX_FILE_SIZE) {
                throw new IOException("File is bigger than %s bytes, which is the maximum size of an AKMC key pair."
                        .formatted(MAX_FILE_SIZE));
            }

            while (true) {
                if (channel.read(inBuffer) <= 0) break;
            }
        }

        inBuffer.flip();

        try {
            int header = inBuffer.getInt();
            Validate.isTrue(
                    header == Constants.KEY_PAIR_HEADER,
                    "Header mismatch: expected 0x%x, got 0x%x.",
                    Constants.KEY_PAIR_HEADER,
                    header);

            short version = inBuffer.getShort();
            Validate.isTrue(
                    version == Constants.KEY_PAIR_VERSION,
                    "Version mismatch: expected %s, got %s.",
                    Constants.KEY_PAIR_VERSION,
                    version);

            short flags = inBuffer.getShort();

            if (flags == Encrypted.ENCRYPTED_FLAG) {
                AkPublicKey publicKey = new AkPublicKey(inBuffer);
                AkEncryptedKey encryptedKey = new AkEncryptedKey(inBuffer);

                return new AkKeyPair.Encrypted(name, modificationTime, publicKey, encryptedKey);
            } else if (flags == Plain.PLAIN_FLAG) {
                AkPrivateKey privateKey = new AkPrivateKey(inBuffer);

                return new AkKeyPair.Plain(name, modificationTime, privateKey);
            } else {
                throw new IllegalArgumentException("Unknown flags value: %x".formatted(flags));
            }
        } catch (BufferUnderflowException e) {
            throw new IOException("Unexpected end of key pair data.", e);
        }
    }

    protected abstract void writeSpecificData(ByteBuffer buffer);

    public static final class Plain extends AkKeyPair {
        private static final short PLAIN_FLAG = 0;

        private final AkPrivateKey privateKey;

        public Plain(String name, Instant modificationTime, AkPrivateKey privateKey) {
            super(name, modificationTime, privateKey.derivePublicKey());

            this.privateKey = privateKey;
        }

        public AkKeyPair.Encrypted encrypt(char[] password) {
            SecureRandom rng;
            try {
                rng = SecureRandom.getInstanceStrong();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            AkEncryptedKey encryptedKey = new AkEncryptedKey(rng, privateKey, password);

            return new Encrypted(name, Instant.now(), publicKey, encryptedKey);
        }

        public AkPrivateKey getPrivateKey() {
            return privateKey;
        }

        @Override
        protected void writeSpecificData(ByteBuffer buffer) {
            buffer.putShort(PLAIN_FLAG);
            privateKey.write(buffer);
        }
    }

    public static final class Encrypted extends AkKeyPair {
        private static final short ENCRYPTED_FLAG = 1;

        private final AkEncryptedKey encryptedKey;

        public Encrypted(String name, Instant modificationTime, AkPublicKey publicKey, AkEncryptedKey encryptedKey) {
            super(name, modificationTime, publicKey);

            this.encryptedKey = encryptedKey;
        }

        public Optional<AkKeyPair.Plain> decrypt(char[] password) {
            return encryptedKey.decrypt(password).map(decrypted -> {
                Validate.isTrue(
                        decrypted.isMatchingPublicKey(publicKey),
                        "Decrypted private key does not match with the current public key.");

                return new AkKeyPair.Plain(name, Instant.now(), decrypted);
            });
        }

        @Override
        protected void writeSpecificData(ByteBuffer buffer) {
            buffer.putShort(ENCRYPTED_FLAG);
            publicKey.write(buffer);
            encryptedKey.write(buffer);
        }
    }
}
