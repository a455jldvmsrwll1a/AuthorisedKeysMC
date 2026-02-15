package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.KeyParameter;

public class AkEncryptedKey {
    private static final int KDF_SALT_LENGTH = 32;
    private static final int KDF_HASH_LENGTH = 32;
    private static final int MAC_LENGTH = 16;
    private static final int MAC_BITS = MAC_LENGTH * 8;
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
        } catch (InvalidCipherTextException e) {
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

    private byte[] decipher(byte[] key) throws InvalidCipherTextException {
        Validate.isTrue(key.length == KDF_HASH_LENGTH, "Wrong key length.");

        byte[] plaintext = new byte[PLAINTEXT_LENGTH];

        AEADParameters aeadParams = new AEADParameters(new KeyParameter(key), MAC_BITS, nonce);

        ChaCha20Poly1305 engine = new ChaCha20Poly1305();
        engine.init(false, aeadParams);

        int outputSize = engine.getOutputSize(OUTPUT_LENGTH);
        Validate.validState(
                outputSize == PLAINTEXT_LENGTH, "Output size of %s does not match what was expected!", outputSize);

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
        Validate.validState(
                outputSize == OUTPUT_LENGTH, "Output size of %s does not match what was expected!", outputSize);

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
