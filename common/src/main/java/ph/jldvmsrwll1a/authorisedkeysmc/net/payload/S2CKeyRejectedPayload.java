package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;

public final class S2CKeyRejectedPayload extends BaseS2CPayload {
    public S2CKeyRejectedPayload() {
        super(QueryPayloadType.SERVER_KEY_REJECTION);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        // Empty.
    }
}
