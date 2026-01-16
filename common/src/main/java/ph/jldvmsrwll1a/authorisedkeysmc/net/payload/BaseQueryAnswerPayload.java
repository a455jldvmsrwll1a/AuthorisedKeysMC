package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import org.apache.commons.lang3.Validate;

public abstract class BaseQueryAnswerPayload implements CustomQueryAnswerPayload {
    public static final int MAGIC = 0x414B4D43;
    private final QueryAnswerPayloadType payloadType;

    protected BaseQueryAnswerPayload(QueryAnswerPayloadType payloadType) {
        this.payloadType = payloadType;
    }

    protected BaseQueryAnswerPayload(FriendlyByteBuf buf, QueryAnswerPayloadType expected) {
        payloadType = readPayloadType(buf);
        Validate.validState(payloadType.equals(expected), "Wrong query anwer payload type! Expected a %s but got a %s!", expected, payloadType);
    }

    @Override
    public final void write(FriendlyByteBuf buf) {
        buf.writeInt(MAGIC);
        buf.writeEnum(payloadType);
        writeData(buf);
    }

    protected abstract void writeData(FriendlyByteBuf buf);

    public static QueryAnswerPayloadType peekPayloadType(FriendlyByteBuf buf) {
        int originalIndex = buf.readerIndex();
        QueryAnswerPayloadType qapType = readPayloadType(buf);
        buf.readerIndex(originalIndex);

        return qapType;
    }

    private static QueryAnswerPayloadType readPayloadType(FriendlyByteBuf buf) {
        int magic = buf.readInt();
        Validate.validState(magic == MAGIC, "Not a valid AKMC query answer payload.");
        return buf.readEnum(QueryAnswerPayloadType.class);
    }
}
