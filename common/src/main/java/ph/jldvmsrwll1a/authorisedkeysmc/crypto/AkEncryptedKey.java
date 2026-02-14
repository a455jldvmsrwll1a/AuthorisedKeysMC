package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;

public class AkEncryptedKey {
    private static final int KDF_SALT_LENGTH = 32;
    private static final int KDF_HASH_LENGTH = 32;
    private static final int MAC_LENGTH = 16;
    private static final int MAC_BITS = MAC_LENGTH * 8;
    private static final int NONCE_LENGTH = 12;
    private static final int PLAINTEXT_LENGTH = AkPrivateKey.LENGTH;
    private static final int OUTPUT_LENGTH = PLAINTEXT_LENGTH + MAC_LENGTH;

    private final int iterations;
    private final int memory;
    private final int parallelism;
    private final byte[] salt;
    private final byte[] nonce;
    private final byte[] cipherText;

    public AkEncryptedKey(SecureRandom random, AkPrivateKey privateKey, char[] password) {
        byte[] key = null;

        try {
            memory = pickMemoryParameter();
            parallelism = pickParallelismParameter();
            iterations = pickIterationParameter(memory, parallelism);

            salt = new byte[KDF_SALT_LENGTH];
            nonce = new byte[NONCE_LENGTH];

            random.nextBytes(salt);
            random.nextBytes(nonce);

            key = deriveKey(password, salt, iterations, memory, parallelism);
            cipherText = encipher(key, nonce, privateKey.getInternal().getEncoded());
        } finally {
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
        }
    }

    public AkEncryptedKey(ByteBuffer buffer) {
        salt = new byte[KDF_SALT_LENGTH];
        nonce = new byte[NONCE_LENGTH];
        cipherText = new byte[OUTPUT_LENGTH];

        iterations = buffer.get();
        parallelism = buffer.get();
        memory = buffer.getShort();

        buffer.get(salt);
        buffer.get(nonce);
        buffer.get(cipherText);
    }

    public void write(ByteBuffer buffer) {
        buffer.put((byte) iterations);
        buffer.put((byte) parallelism);
        buffer.putShort((short) memory);

        buffer.put(salt);
        buffer.put(nonce);
        buffer.put(cipherText);
    }

    public Optional<AkPrivateKey> decrypt(char[] password) {
        byte[] key = null;

        try {
            key = deriveKey(password, salt, iterations, memory, parallelism);
            byte[] decryptedBytes = decipher(key);

            return Optional.of(new AkPrivateKey(decryptedBytes));
        } catch (InvalidCipherTextException e) {
            return Optional.empty();
        } finally {
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations, int memory, int parallelism) {
        Validate.isTrue(salt.length == KDF_SALT_LENGTH, "Wrong salt length.");

        int memoryKiB = memory * 1024;

        byte[] key = new byte[KDF_HASH_LENGTH];

        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iterations)
                .withMemoryAsKB(memoryKiB)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(parameters);
        generator.generateBytes(password, key);

        return key;
    }

    private byte[] decipher(byte[] key) throws InvalidCipherTextException {
        Validate.isTrue(key.length == KDF_HASH_LENGTH, "Wrong key length.");

        byte[] plaintext = new byte[PLAINTEXT_LENGTH];

        AEADParameters aeadParams = new AEADParameters(new KeyParameter(key), MAC_BITS, nonce);

        ChaCha20Poly1305 engine = new ChaCha20Poly1305();
        engine.init(false, aeadParams);

        int outputSize = engine.getOutputSize(OUTPUT_LENGTH);
        Validate.validState(outputSize == PLAINTEXT_LENGTH, "Output size of %s does not match what was expected!", outputSize);

        int head = engine.processBytes(cipherText, 0, OUTPUT_LENGTH, plaintext, 0);
        head += engine.doFinal(plaintext, head);

        Validate.validState(head == PLAINTEXT_LENGTH, "Head should match expected size!");

        return plaintext;
    }

    private static byte[] encipher(byte[] key, byte[] nonce, byte[] plaintext) {
        Validate.isTrue(key.length == KDF_HASH_LENGTH, "Wrong key length.");
        Validate.isTrue(nonce.length == NONCE_LENGTH, "Wrong nonce length.");
        Validate.isTrue(plaintext.length == PLAINTEXT_LENGTH, "Wrong plaintext length.");

        byte[] cipherText = new byte[OUTPUT_LENGTH];

        AEADParameters aeadParams = new AEADParameters(new KeyParameter(key), MAC_BITS, nonce);

        ChaCha20Poly1305 engine = new ChaCha20Poly1305();
        engine.init(true, aeadParams);

        int outputSize = engine.getOutputSize(AkPrivateKey.LENGTH);
        Validate.validState(outputSize == OUTPUT_LENGTH, "Output size of %s does not match what was expected!", outputSize);

        int head = engine.processBytes(plaintext, 0, PLAINTEXT_LENGTH, cipherText, 0);

        try {
            head += engine.doFinal(cipherText, head);
        } catch (InvalidCipherTextException e) {
            // InvalidCipherTextException is only ever thrown when decrypting, which we are not.
            throw new RuntimeException("Should not be possible as we are encrypting.", e);
        }

        Validate.validState(head == OUTPUT_LENGTH, "Head should match expected size!");

        return cipherText;
    }

    private static int pickIterationParameter(int memory, int parallelism) {
        int i = 8;

        if (parallelism > 4) {
            i *= 2;
        }

        i /= memory / 128;

        return Math.clamp(i, 2, 16);
    }

    private static int pickMemoryParameter() {
        Runtime runtime = Runtime.getRuntime();

        long allocated = runtime.totalMemory() - runtime.freeMemory();
        long available = runtime.maxMemory() - allocated;
        long mib = available / 1048576;

        return Math.clamp(mib, 32, 512);
    }

    private static int pickParallelismParameter() {
        int cores = Runtime.getRuntime().availableProcessors();

        return Math.clamp(cores / 3, 1, 8);
    }
}
