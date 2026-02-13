package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;

public final class C2SPingPayload extends BaseC2SPayload {
    public C2SPingPayload() {
        super(QueryAnswerPayloadType.PING);
    }

    public C2SPingPayload(FriendlyByteBuf buf) {
        super(buf, QueryAnswerPayloadType.PING);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        // Empty.
    }
}
