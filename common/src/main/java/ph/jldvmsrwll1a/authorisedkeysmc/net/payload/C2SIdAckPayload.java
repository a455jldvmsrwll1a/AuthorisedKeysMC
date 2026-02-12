package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;

public class C2SIdAckPayload extends BaseC2SPayload {
    public C2SIdAckPayload() {
        super(QueryAnswerPayloadType.ID_ACK);
    }

    public C2SIdAckPayload(FriendlyByteBuf buf) {
        super(buf, QueryAnswerPayloadType.ID_ACK);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        // Empty.
    }
}
