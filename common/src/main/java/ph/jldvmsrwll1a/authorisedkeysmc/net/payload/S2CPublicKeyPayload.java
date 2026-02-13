package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.Validate;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;

public final class S2CPublicKeyPayload extends BaseS2CPayload {
    private static final int KEY_BYTES_LENGTH = AkPublicKey.ENCODED_LENGTH;

    public final AkPublicKey key;

    public S2CPublicKeyPayload(AkPublicKey key) {
        super(QueryPayloadType.SERVER_KEY);
        this.key = key;
    }

    public S2CPublicKeyPayload(FriendlyByteBuf buf) {
        super(buf, QueryPayloadType.SERVER_KEY);

        byte[] bytes = new byte[KEY_BYTES_LENGTH];
        Validate.validState(
                buf.readableBytes() == KEY_BYTES_LENGTH, "Encoded public key is not %s bytes long!", KEY_BYTES_LENGTH);
        buf.readBytes(KEY_BYTES_LENGTH).readBytes(bytes);
        key = new AkPublicKey(bytes);
    }

    @Override
    public void writeData(FriendlyByteBuf buf) {
        byte[] bytes = key.getEncoded();
        buf.writeBytes(bytes);
    }
}
