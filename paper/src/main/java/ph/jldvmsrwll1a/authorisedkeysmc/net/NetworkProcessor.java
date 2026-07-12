package ph.jldvmsrwll1a.authorisedkeysmc.net;

import io.netty.channel.Channel;

public final class NetworkProcessor {
    private static final String HANDLER_NAME = "akmc_net_handler";

    public void inject(Channel channel) {
        channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new PacketInterceptor(channel, this));
    }

    public void uninject(Channel channel) {
        if (channel.pipeline().get(HANDLER_NAME) != null) {
            channel.pipeline().remove(HANDLER_NAME);
        }
    }
}
