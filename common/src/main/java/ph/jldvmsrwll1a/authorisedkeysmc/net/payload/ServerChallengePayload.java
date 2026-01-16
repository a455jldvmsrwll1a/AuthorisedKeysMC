package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.Validate;

import java.security.SecureRandom;

public final class ServerChallengePayload extends BaseQueryPayload {
    public static final int NONCE_LENGTH = 256;

    private final byte[] nonce;

    public ServerChallengePayload() {
        super(QueryPayloadType.SERVER_CHALLENGE);

        SecureRandom rng = new SecureRandom();
        nonce = new byte[NONCE_LENGTH];
        rng.nextBytes(nonce);
    }

    public ServerChallengePayload(FriendlyByteBuf buf) {
        super(buf, QueryPayloadType.SERVER_CHALLENGE);

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
