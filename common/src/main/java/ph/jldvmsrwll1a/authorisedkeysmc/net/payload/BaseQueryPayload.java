package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.NonNull;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

public abstract class BaseQueryPayload implements CustomQueryPayload {
    public static final int MAGIC = 0x414B4D43;
    private final QueryPayloadType payloadType;

    protected BaseQueryPayload(QueryPayloadType payloadType) {
        this.payloadType = payloadType;
    }

    protected BaseQueryPayload(FriendlyByteBuf buf, QueryPayloadType expected) {
        payloadType = readPayloadType(buf);
        Validate.validState(payloadType.equals(expected), "Wrong query payload type! Expected a %s but got a %s!", expected, payloadType);
    }

    @Override
    public final @NonNull Identifier id() {
        return Constants.LOGIN_CHANNEL_ID;
    }

    @Override
    public final void write(FriendlyByteBuf buf) {
        buf.writeInt(MAGIC);
        buf.writeEnum(payloadType);
        writeData(buf);
    }

    protected abstract void writeData(FriendlyByteBuf buf);

    public static QueryPayloadType peekPayloadType(FriendlyByteBuf buf) {
        int originalIndex = buf.readerIndex();
        QueryPayloadType qpType = readPayloadType(buf);
        buf.readerIndex(originalIndex);

        return qpType;
    }

    private static QueryPayloadType readPayloadType(FriendlyByteBuf buf) {
        int magic = buf.readInt();
        Validate.validState(magic == MAGIC, "Not a valid AKMC query payload.");
        return buf.readEnum(QueryPayloadType.class);
    }
}
