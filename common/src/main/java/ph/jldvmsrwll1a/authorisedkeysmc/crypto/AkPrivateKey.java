package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.security.SecureRandom;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;

public final class AkPrivateKey {
    private final Ed25519PrivateKeyParameters parameters;

    public AkPrivateKey(SecureRandom random) {
        parameters = new Ed25519PrivateKeyParameters(random);
    }

    public AkPrivateKey(byte[] bytes) {
        parameters = new Ed25519PrivateKeyParameters(bytes);
    }

    public AkPublicKey derivePublicKey() {
        return new AkPublicKey(this);
    }

    Ed25519PrivateKeyParameters getInternal() {
        return parameters;
    }
}
