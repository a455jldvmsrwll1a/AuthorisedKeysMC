package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AkEncryptedKey {
    private static final String KEY_ALGORITHM = "ChaCha20";
    private static final String CIPHER_ALGORITHM = "ChaCha20-Poly1305";
    private static final int KDF_SALT_LENGTH = 32;
    private static final int KDF_HASH_LENGTH = 32;
    private static final int MAC_LENGTH = 16;
    private static final int NONCE_LENGTH = 12;
    private static final int PLAINTEXT_LENGTH = AkPrivateKey.LENGTH;
    private static final int OUTPUT_LENGTH = PLAINTEXT_LENGTH + MAC_LENGTH;

    private static final int DEFAULT_TARGET_MS = 900;

    private final Parameters parameters;
    private final byte[] salt;
    private final byte[] nonce;
    private final byte[] cipherText;

    public AkEncryptedKey(SecureRandom random, AkPrivateKey privateKey, char[] password) {
        this(random, privateKey, Parameters.calibrate(DEFAULT_TARGET_MS), password);
    }

    public AkEncryptedKey(SecureRandom random, AkPrivateKey privateKey, Parameters parameters, char[] password) {
        this.parameters = parameters;

        byte[] key = null;

        try {
            salt = new byte[KDF_SALT_LENGTH];
            nonce = new byte[NONCE_LENGTH];

            random.nextBytes(salt);
            random.nextBytes(nonce);

            key = deriveKey(password, salt, parameters.iterations, parameters.memory, parameters.parallelism);
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

        byte iterations = buffer.get();
        byte parallelism = buffer.get();
        short memoryMiB = buffer.getShort();

        parameters = new Parameters(iterations, memoryMiB * 1024, parallelism);

        buffer.get(salt);
        buffer.get(nonce);
        buffer.get(cipherText);
    }

    public void write(ByteBuffer buffer) {
        short memoryMiB = (short) (parameters.memory / 1024);

        buffer.put((byte) parameters.iterations);
        buffer.put((byte) parameters.parallelism);
        buffer.putShort(memoryMiB);

        buffer.put(salt);
        buffer.put(nonce);
        buffer.put(cipherText);
    }

    public Optional<AkPrivateKey> decrypt(char[] password) {
        byte[] key = null;

        try {
            key = deriveKey(password, salt, parameters.iterations, parameters.memory, parameters.parallelism);

            byte[] decryptedBytes = decipher(key);

            return Optional.of(new AkPrivateKey(decryptedBytes));
        } catch (InvalidKeyException | BadPaddingException e) {
            Constants.LOG.info("Failed to decrypt the private key: {}", e.getMessage());

            return Optional.empty();
        } finally {
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int t, int m, int p) {
        byte[] key = new byte[KDF_HASH_LENGTH];
        deriveKey(password, salt, t, m, p, key);
        return key;
    }

    private static void deriveKey(char[] password, byte[] salt, int t, int m, int p, byte[] out) {
        Validate.isTrue(salt.length == KDF_SALT_LENGTH, "Wrong salt length.");
        Validate.isTrue(out.length == KDF_HASH_LENGTH, "Wrong output buffer length.");

        Argon2Parameters argon2Parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(t)
                .withMemoryAsKB(m)
                .withParallelism(p)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(argon2Parameters);
        generator.generateBytes(password, out);
    }

    private byte[] decipher(byte[] key) throws InvalidKeyException, BadPaddingException {
        Validate.isTrue(key.length == KDF_HASH_LENGTH, "Wrong key length.");

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, KEY_ALGORITHM), new IvParameterSpec(nonce));
            byte[] plaintext = cipher.doFinal(cipherText);
            Validate.validState(plaintext.length == PLAINTEXT_LENGTH, "Decrypted ciphertext is not of the expected size.");

            return plaintext;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] encipher(byte[] key, byte[] nonce, byte[] plaintext) {
        Validate.isTrue(key.length == KDF_HASH_LENGTH, "Wrong key length.");
        Validate.isTrue(nonce.length == NONCE_LENGTH, "Wrong nonce length.");
        Validate.isTrue(plaintext.length == PLAINTEXT_LENGTH, "Wrong plaintext length.");

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, KEY_ALGORITHM), new IvParameterSpec(nonce));
            byte[] ciphertext = cipher.doFinal(plaintext);
            Validate.validState(ciphertext.length == OUTPUT_LENGTH, "Encrypted plaintext is not of the expected size.");

            return ciphertext;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    public static final class Parameters {
        private static final char[] PASSWORD = "balls oyster 123".toCharArray();

        private final int iterations;
        private final int memory;
        private final int parallelism;

        public Parameters(int t, int m, int p) {
            iterations = t;
            memory = m;
            parallelism = p;
        }

        public static Parameters calibrate(int targetMillis) {
            byte[] salt = generateInsecureSalt();
            byte[] output = new byte[KDF_HASH_LENGTH];

            int memory = pickMemoryParameter();
            int parallelism = pickParallelismParameter();

            // Warm-up.
            deriveKey(PASSWORD, salt, 1, memory, parallelism, output);
            deriveKey(PASSWORD, salt, 1, memory, parallelism, output);

            // Measure time taken for t = 1, 2, and 3.

            long start = System.nanoTime();
            deriveKey(PASSWORD, salt, 1, memory, parallelism, output);
            long end = System.nanoTime();
            double t1 = (double) (end - start) / 1000.0;

            start = System.nanoTime();
            deriveKey(PASSWORD, salt, 2, memory, parallelism, output);
            end = System.nanoTime();
            double t2 = (double) (end - start) / 1000.0;

            start = System.nanoTime();
            deriveKey(PASSWORD, salt, 3, memory, parallelism, output);
            end = System.nanoTime();
            double t3 = (double) (end - start) / 1000.0;

            // Linear regression for 3 points via least squares method.

            double xs = 6.0;
            double ys = t1 + t2 + t3;
            double xys = t1 + (2.0 * t2) + (3.0 * t3);
            double xxs = 14.0;

            // Find the linear equation.
            double slope = (3.0 * xys - xs * ys) / (3.0 * xxs - xs * xs);
            double intercept = (ys - slope * xs) / 3.0;

            double target = (double) targetMillis * 1000.0;
            // Solve for predicted t parameter with the inverse of the equation.
            double predicted = (target - intercept) / slope;

            int iterations = (int) Math.ceil(Math.clamp(predicted, 1.0, 32.0));

            Arrays.fill(output, (byte) 0);
            return new Parameters(iterations, memory, parallelism);
        }

        private static int pickMemoryParameter() {
            Runtime runtime = Runtime.getRuntime();

            long allocated = runtime.totalMemory() - runtime.freeMemory();
            long available = runtime.maxMemory() - allocated;
            long mib = available / 1048576;

            return Math.clamp(mib / 4, 64, 512) * 1024;
        }

        private static int pickParallelismParameter() {
            int cores = Runtime.getRuntime().availableProcessors();

            return Math.clamp(cores / 3, 1, 16);
        }

        private static byte[] generateInsecureSalt() {
            byte[] salt = new byte[KDF_SALT_LENGTH];

            Random random = new Random();
            random.nextBytes(salt);

            return salt;
        }
    }
}
