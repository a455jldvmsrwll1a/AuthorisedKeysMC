package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

public final class S2CSignaturePayload extends BaseS2CPayload {
    public static final int SIGNATURE_LENGTH = Ed25519PrivateKeyParameters.SIGNATURE_SIZE;

    public final byte[] signature;

    public static S2CSignaturePayload fromSigningChallenge(Ed25519PrivateKeyParameters signingKey, C2SChallengePayload challenge) {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, signingKey);
        signer.update(challenge.getNonce(), 0, C2SChallengePayload.NONCE_LENGTH);

        return new S2CSignaturePayload(signer.generateSignature());
    }

    public S2CSignaturePayload(byte[] signature) {
        super(QueryPayloadType.CLIENT_CHALLENGE_RESPONSE);

        Validate.isTrue(signature.length == SIGNATURE_LENGTH);
        this.signature = signature;
    }

    public S2CSignaturePayload(FriendlyByteBuf buf) {
        super(buf, QueryPayloadType.CLIENT_CHALLENGE_RESPONSE);

        Validate.isTrue(buf.readableBytes() == SIGNATURE_LENGTH, "Signatures must be exactly %s bytes.", SIGNATURE_LENGTH);
        signature = new byte[SIGNATURE_LENGTH];
        buf.readBytes(SIGNATURE_LENGTH).readBytes(signature);
    }

    public boolean verify(Ed25519PublicKeyParameters verifyingKey, byte[] nonce) {
        Validate.isTrue(nonce.length == S2CChallengePayload.NONCE_LENGTH, "Nonce must be %s bytes.", S2CChallengePayload.NONCE_LENGTH);

        Ed25519Signer signer = new Ed25519Signer();
        signer.init(false, verifyingKey);
        signer.update(nonce, 0, S2CChallengePayload.NONCE_LENGTH);

        return signer.verifySignature(signature);
    }

    @Override
    public void writeData(FriendlyByteBuf buf) {
        buf.writeBytes(signature);
    }
}
