package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

public final class AkSigner {
    public static final int SIGNATURE_LENGTH = Ed25519PrivateKeyParameters.SIGNATURE_SIZE;

    private final Ed25519Signer signer;

    public AkSigner(AkPrivateKey signingKey) {
        signer = new Ed25519Signer();
        signer.init(true, signingKey.getInternal());
    }

    public void update(byte b) {
        signer.update(b);
    }

    public void update(byte[] bytes) {
        update(bytes, bytes.length);
    }

    public void update(byte[] bytes, int expectedLength) {
        Validate.isTrue(bytes.length == expectedLength, "Updating signer with buffer of wrong size.");

        signer.update(bytes, 0, expectedLength);
    }

    public byte[] sign() {
        return signer.generateSignature();
    }
}
