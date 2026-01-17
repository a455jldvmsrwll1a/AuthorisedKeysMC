package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

public final class C2SSignaturePayload extends BaseC2SPayload {
    private static final int SIGNATURE_LENGTH = Ed25519PrivateKeyParameters.SIGNATURE_SIZE;

    public final byte[] signature;

    public static C2SSignaturePayload fromSigningChallenge(Ed25519PrivateKeyParameters signingKey, S2CChallengePayload challenge) {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, signingKey);
        signer.update(challenge.getNonce(), 0, C2SChallengePayload.NONCE_LENGTH);

        return new C2SSignaturePayload(signer.generateSignature());
    }

    public C2SSignaturePayload(byte[] signature) {
        super(QueryAnswerPayloadType.SERVER_CHALLENGE_RESPONSE);

        Validate.isTrue(signature.length == SIGNATURE_LENGTH);
        this.signature = signature;
    }

    public C2SSignaturePayload(FriendlyByteBuf buf) {
        super(buf, QueryAnswerPayloadType.SERVER_CHALLENGE_RESPONSE);

        Validate.isTrue(buf.readableBytes() == SIGNATURE_LENGTH, "Signatures must be exactly %s bytes.", SIGNATURE_LENGTH);
        signature = new byte[SIGNATURE_LENGTH];
        buf.readBytes(SIGNATURE_LENGTH).readBytes(signature);
    }

    public boolean verify(Ed25519PublicKeyParameters verifyingKey, byte[] nonce) {
        Validate.isTrue(nonce.length == C2SChallengePayload.NONCE_LENGTH, "Nonce must be %s bytes.", C2SChallengePayload.NONCE_LENGTH);

        Ed25519Signer signer = new Ed25519Signer();
        signer.init(false, verifyingKey);
        signer.update(nonce, 0, C2SChallengePayload.NONCE_LENGTH);

        return signer.verifySignature(signature);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeBytes(signature);
    }
}
