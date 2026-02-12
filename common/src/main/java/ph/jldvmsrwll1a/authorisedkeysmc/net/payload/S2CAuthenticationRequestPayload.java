package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;

public final class S2CAuthenticationRequestPayload extends BaseS2CPayload {
    public S2CAuthenticationRequestPayload() {
        super(QueryPayloadType.AUTHENTICATION_REQUEST);
    }

    public S2CAuthenticationRequestPayload(FriendlyByteBuf buf) {
        super(buf, QueryPayloadType.AUTHENTICATION_REQUEST);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        // Empty.
    }
}
