package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import java.security.SecureRandom;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.Validate;

public final class C2SChallengePayload extends BaseC2SPayload {
    public static final int NONCE_LENGTH = 256;
    private final byte[] nonce;

    public C2SChallengePayload() {
        super(QueryAnswerPayloadType.CLIENT_CHALLENGE);

        SecureRandom rng = new SecureRandom();

        nonce = new byte[NONCE_LENGTH];
        rng.nextBytes(nonce);
    }

    public C2SChallengePayload(FriendlyByteBuf buf) {
        super(buf, QueryAnswerPayloadType.CLIENT_CHALLENGE);

        Validate.validState(buf.readableBytes() == NONCE_LENGTH, "Incorrect size of nonce.");
        nonce = new byte[NONCE_LENGTH];
        buf.readBytes(NONCE_LENGTH).readBytes(nonce);
    }

    public byte[] getNonce() {
        return nonce;
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeBytes(nonce);
    }
}
