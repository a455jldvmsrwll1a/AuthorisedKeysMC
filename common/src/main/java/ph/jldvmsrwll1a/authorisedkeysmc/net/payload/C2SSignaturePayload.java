package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import java.nio.charset.StandardCharsets;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

public final class C2SSignaturePayload extends BaseC2SPayload {
    private static final int SIGNATURE_LENGTH = Ed25519PrivateKeyParameters.SIGNATURE_SIZE;
    private static final byte[] PREFIX = "Authorised Keys MC".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MARKER_LOGIN = "Log-in".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MARKER_REGISTRATION = "Registration".getBytes(StandardCharsets.US_ASCII);
    private static final int VERSION = Constants.PROTOCOL_VERSION;

    public final byte[] signature;

    public static C2SSignaturePayload fromSigningChallenge(
            Ed25519PrivateKeyParameters signingKey,
            S2CChallengePayload challenge,
            byte[] sessionHash,
            boolean forRegistration) {
        byte[] marker = forRegistration ? MARKER_REGISTRATION : MARKER_LOGIN;

        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, signingKey);
        signer.update(PREFIX, 0, PREFIX.length);
        signer.update((byte) (VERSION >> 24));
        signer.update((byte) (VERSION >> 16));
        signer.update((byte) (VERSION >> 8));
        signer.update((byte) (VERSION));
        signer.update(sessionHash, 0, sessionHash.length);
        signer.update(marker, 0, marker.length);
        signer.update(challenge.getNonce(), 0, S2CChallengePayload.NONCE_LENGTH);

        return new C2SSignaturePayload(signer.generateSignature());
    }

    public C2SSignaturePayload(byte[] signature) {
        super(QueryAnswerPayloadType.SERVER_CHALLENGE_RESPONSE);

        Validate.isTrue(signature.length == SIGNATURE_LENGTH);
        this.signature = signature;
    }

    public C2SSignaturePayload(FriendlyByteBuf buf) {
        super(buf, QueryAnswerPayloadType.SERVER_CHALLENGE_RESPONSE);

        Validate.isTrue(
                buf.readableBytes() == SIGNATURE_LENGTH, "Signatures must be exactly %s bytes.", SIGNATURE_LENGTH);
        signature = new byte[SIGNATURE_LENGTH];
        buf.readBytes(SIGNATURE_LENGTH).readBytes(signature);
    }

    public boolean verify(
            Ed25519PublicKeyParameters verifyingKey, byte[] nonce, byte[] sessionHash, boolean forRegistration) {
        byte[] marker = forRegistration ? MARKER_REGISTRATION : MARKER_LOGIN;

        Validate.isTrue(
                nonce.length == S2CChallengePayload.NONCE_LENGTH,
                "Nonce must be %s bytes.",
                C2SChallengePayload.NONCE_LENGTH);

        Ed25519Signer signer = new Ed25519Signer();
        signer.init(false, verifyingKey);
        signer.update(PREFIX, 0, PREFIX.length);
        signer.update((byte) (VERSION >> 24));
        signer.update((byte) (VERSION >> 16));
        signer.update((byte) (VERSION >> 8));
        signer.update((byte) (VERSION));
        signer.update(sessionHash, 0, sessionHash.length);
        signer.update(marker, 0, marker.length);
        signer.update(nonce, 0, S2CChallengePayload.NONCE_LENGTH);

        return signer.verifySignature(signature);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeBytes(signature);
    }
}
