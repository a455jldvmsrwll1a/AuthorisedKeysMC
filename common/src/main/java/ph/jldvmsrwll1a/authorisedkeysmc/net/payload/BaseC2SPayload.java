package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import org.apache.commons.lang3.Validate;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

public abstract class BaseC2SPayload implements CustomQueryAnswerPayload {
    private final QueryAnswerPayloadType payloadType;

    protected BaseC2SPayload(QueryAnswerPayloadType payloadType) {
        this.payloadType = payloadType;
    }

    protected BaseC2SPayload(FriendlyByteBuf buf, QueryAnswerPayloadType expected) {
        payloadType = readPayloadType(buf);
        Validate.validState(payloadType.equals(expected), "Wrong query answer payload type! Expected a %s but got a %s!", expected, payloadType);
    }

    @Override
    public final void write(FriendlyByteBuf buf) {
        buf.writeInt(Constants.PAYLOAD_HEADER);
        buf.writeVarInt(Constants.PROTOCOL_VERSION);
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
        Validate.validState(magic == Constants.PAYLOAD_HEADER, "Not a valid AKMC query answer payload.");
        int version = buf.readVarInt();
        Validate.validState(version == Constants.PROTOCOL_VERSION, "Unsupported AKMC query answer payload version.");
        return buf.readEnum(QueryAnswerPayloadType.class);
    }
}
