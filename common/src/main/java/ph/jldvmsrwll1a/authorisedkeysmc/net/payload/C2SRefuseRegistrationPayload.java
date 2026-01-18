package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;

public class C2SRefuseRegistrationPayload extends BaseC2SPayload {
    public C2SRefuseRegistrationPayload() {
        super(QueryAnswerPayloadType.WONT_REGISTER);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        // Empty.
    }
}
