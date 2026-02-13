package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;

public final class S2CPongPayload extends BaseS2CPayload {
    public S2CPongPayload() {
        super(QueryPayloadType.PONG);
    }

    public S2CPongPayload(FriendlyByteBuf buf) {
        super(buf, QueryPayloadType.PONG);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        // Empty.
    }
}
