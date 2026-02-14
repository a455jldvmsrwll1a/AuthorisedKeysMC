package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;

public final class AkPrivateKey {
    public static final int LENGTH = Ed25519PrivateKeyParameters.KEY_SIZE;

    private final Ed25519PrivateKeyParameters parameters;

    public AkPrivateKey(SecureRandom random) {
        parameters = new Ed25519PrivateKeyParameters(random);
    }

    public AkPrivateKey(ByteBuffer buffer) {
        byte[] bytes = new byte[LENGTH];
        buffer.get(bytes);
        parameters = new Ed25519PrivateKeyParameters(bytes);
    }

    public AkPrivateKey(byte[] bytes) {
        parameters = new Ed25519PrivateKeyParameters(bytes);
    }

    public void write(ByteBuffer buffer) {
        buffer.put(parameters.getEncoded());
    }

    public AkPublicKey derivePublicKey() {
        return new AkPublicKey(this);
    }

    Ed25519PrivateKeyParameters getInternal() {
        return parameters;
    }
}
