package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.signers.Ed25519Signer;

public class AkVerifier {
    private final Ed25519Signer verifier;

    public AkVerifier(AkPublicKey verifyingKey) {
        verifier = new Ed25519Signer();
        verifier.init(false, verifyingKey.getInternal());
    }

    public void update(byte b) {
        verifier.update(b);
    }

    public void update(byte[] bytes) {
        update(bytes, bytes.length);
    }

    public void update(byte[] bytes, int expectedLength) {
        Validate.isTrue(bytes.length == expectedLength, "Updating verifier with buffer of wrong size.");

        verifier.update(bytes, 0, expectedLength);
    }

    public boolean verify(byte[] signature) {
        return verifier.verifySignature(signature);
    }
}
