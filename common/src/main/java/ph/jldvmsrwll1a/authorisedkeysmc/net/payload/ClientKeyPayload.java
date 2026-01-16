package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.math.ec.rfc8032.Ed25519;

public class ClientKeyPayload extends BaseQueryAnswerPayload {
    private static final int KEY_BYTES_LENGTH = Ed25519.PUBLIC_KEY_SIZE;

    public final Ed25519PublicKeyParameters key;

    public ClientKeyPayload(Ed25519PublicKeyParameters key) {
        super(QueryAnswerPayloadType.CLIENT_KEY);
        this.key = key;
    }

    public ClientKeyPayload(FriendlyByteBuf buf) {
        super(buf, QueryAnswerPayloadType.CLIENT_KEY);

        byte[] bytes = new byte[KEY_BYTES_LENGTH];
        Validate.validState(buf.readableBytes() == KEY_BYTES_LENGTH, "Encoded public key is not %s bytes long!", KEY_BYTES_LENGTH);
        buf.readBytes(KEY_BYTES_LENGTH).readBytes(bytes);
        key = new Ed25519PublicKeyParameters(bytes);
    }

    @Override
    protected void writeData(FriendlyByteBuf buf) {
        byte[] bytes = key.getEncoded();
        buf.writeBytes(bytes);
    }
}
