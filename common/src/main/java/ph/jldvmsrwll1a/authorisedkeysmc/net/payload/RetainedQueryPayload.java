package ph.jldvmsrwll1a.authorisedkeysmc.net.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

import java.util.function.Consumer;

public record RetainedQueryPayload(Identifier id, FriendlyByteBuf buf, Consumer<FriendlyByteBuf> encoder) implements CustomQueryPayload {
    public RetainedQueryPayload(Identifier id, FriendlyByteBuf contents) {
        this(id, contents, inBuf -> {
            Constants.LOG.warn("RetainedQueryPayload no-op encoder called.");
        });
    }

    @Override
    public @NotNull Identifier id() {
        return id;
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
