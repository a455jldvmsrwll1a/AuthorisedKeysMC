package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import java.nio.charset.StandardCharsets;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.Validate;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPrivateKey;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkSigner;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkVerifier;

public final class S2CSignaturePayload extends BaseS2CPayload {
    public static final int SIGNATURE_LENGTH = AkSigner.SIGNATURE_LENGTH;

    private static final byte[] PREFIX = "Authorised Keys MC Server Identity".getBytes(StandardCharsets.US_ASCII);
    private static final int VERSION = Constants.PROTOCOL_VERSION;

    public final byte[] signature;

    public static S2CSignaturePayload fromSigningChallenge(
            AkPrivateKey signingKey, C2SChallengePayload challenge, byte[] sessionHash) {
        AkSigner signer = new AkSigner(signingKey);
        signer.update(PREFIX);
        signer.update((byte) (VERSION >> 24));
        signer.update((byte) (VERSION >> 16));
        signer.update((byte) (VERSION >> 8));
        signer.update((byte) (VERSION));
        signer.update(sessionHash);
        signer.update(challenge.getNonce(), C2SChallengePayload.NONCE_LENGTH);

        return new S2CSignaturePayload(signer.sign());
    }

    public S2CSignaturePayload(byte[] signature) {
        super(QueryPayloadType.CLIENT_CHALLENGE_RESPONSE);

        Validate.isTrue(signature.length == SIGNATURE_LENGTH);
        this.signature = signature;
    }

    public S2CSignaturePayload(FriendlyByteBuf buf) {
        super(buf, QueryPayloadType.CLIENT_CHALLENGE_RESPONSE);

        Validate.isTrue(
                buf.readableBytes() == SIGNATURE_LENGTH, "Signatures must be exactly %s bytes.", SIGNATURE_LENGTH);
        signature = new byte[SIGNATURE_LENGTH];
        buf.readBytes(SIGNATURE_LENGTH).readBytes(signature);
    }

    public boolean verify(AkPublicKey verifyingKey, byte[] nonce, byte[] sessionHash) {
        Validate.isTrue(
                nonce.length == S2CChallengePayload.NONCE_LENGTH,
                "Nonce must be %s bytes.",
                S2CChallengePayload.NONCE_LENGTH);

        AkVerifier verifier = new AkVerifier(verifyingKey);
        verifier.update(PREFIX);
        verifier.update((byte) (VERSION >> 24));
        verifier.update((byte) (VERSION >> 16));
        verifier.update((byte) (VERSION >> 8));
        verifier.update((byte) (VERSION));
        verifier.update(sessionHash);
        verifier.update(nonce, C2SChallengePayload.NONCE_LENGTH);

        return verifier.verify(signature);
    }

    @Override
    public void writeData(FriendlyByteBuf buf) {
        buf.writeBytes(signature);
    }
}
