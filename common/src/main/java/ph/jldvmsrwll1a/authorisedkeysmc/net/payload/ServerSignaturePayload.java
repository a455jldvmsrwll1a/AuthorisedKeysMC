package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

public class ServerSignaturePayload extends BaseQueryPayload {
    private static final int SIGNATURE_LENGTH = Ed25519PrivateKeyParameters.SIGNATURE_SIZE;

    public final byte[] signature;

    public static ServerSignaturePayload fromSigningChallenge(Ed25519PrivateKeyParameters signingKey, ClientChallengePayload challenge) {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, signingKey);
        signer.update(challenge.getNonce(), 0, ClientChallengePayload.NONCE_LENGTH);

        return new ServerSignaturePayload(signer.generateSignature());
    }

    public ServerSignaturePayload(byte[] signature) {
        super(QueryPayloadType.CLIENT_CHALLENGE_RESPONSE);
        this.signature = signature;
    }

    public ServerSignaturePayload(FriendlyByteBuf buf) {
        super(buf, QueryPayloadType.CLIENT_CHALLENGE_RESPONSE);

        Validate.validState(buf.readableBytes() == SIGNATURE_LENGTH, "Signatures must be exactly %s bytes.", SIGNATURE_LENGTH);
        signature = new byte[SIGNATURE_LENGTH];
        buf.readBytes(SIGNATURE_LENGTH).readBytes(signature);
    }

    @Override
    public void writeData(FriendlyByteBuf buf) {
        buf.writeBytes(signature);
    }
}
