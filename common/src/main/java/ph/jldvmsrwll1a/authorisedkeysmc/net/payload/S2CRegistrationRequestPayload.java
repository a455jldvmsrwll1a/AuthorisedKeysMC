package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;

public class S2CRegistrationRequestPayload extends BaseS2CPayload {
    public S2CRegistrationRequestPayload() {
        super(QueryPayloadType.REGISTRATION_REQUEST);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        // Empty.
    }
}
