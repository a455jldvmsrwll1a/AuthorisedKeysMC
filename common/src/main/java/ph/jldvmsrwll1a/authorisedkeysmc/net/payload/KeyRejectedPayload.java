package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;

public final class KeyRejectedPayload extends BaseQueryPayload {
    public KeyRejectedPayload() {
        super(QueryPayloadType.SERVER_KEY_REJECTION);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        // Empty.
    }
}
