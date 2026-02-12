package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;

public final class S2CRegistrationRequestPayload extends BaseS2CPayload {
    private final boolean required;

    public S2CRegistrationRequestPayload(boolean required) {
        super(QueryPayloadType.REGISTRATION_REQUEST);

        this.required = required;
    }

    public S2CRegistrationRequestPayload(FriendlyByteBuf buf) {
        super(buf, QueryPayloadType.REGISTRATION_REQUEST);

        required = buf.readBoolean();
    }

    public boolean registrationRequired() {
        return required;
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        buf.writeBoolean(required);
    }
}
