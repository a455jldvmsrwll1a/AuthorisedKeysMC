package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import org.jetbrains.annotations.NotNull;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

public record RetainedQueryAnswerPayload(FriendlyByteBuf buf, Consumer<FriendlyByteBuf> encoder)
        implements CustomQueryAnswerPayload {
    public RetainedQueryAnswerPayload(FriendlyByteBuf contents) {
        this(contents, inBuf -> {
            Constants.LOG.warn("RetainedAnswerQueryPayload no-op encoder called.");
        });
    }

    @Override
    public void write(@NotNull FriendlyByteBuf outBuf) {
        if (buf == null) {
            encoder.accept(outBuf);
        } else {
            outBuf.writeBytes(buf);
        }
    }
}
