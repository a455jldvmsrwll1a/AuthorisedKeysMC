package ph.jldvmsrwll1a.authorisedkeysmc;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.ForgePlatformHelper;

@Mod(Constants.MOD_ID)
public class AuthorisedKeysMC {
    public AuthorisedKeysMC() {
        MinecraftForge.EVENT_BUS.register(this);

        AuthorisedKeysModCore.init(new ForgePlatformHelper());

        if (FMLLoader.getDist().isClient()) {
            AuthorisedKeysModClient.init();
        }
    }

    @SubscribeEvent
    private void onTick(TickEvent.ClientTickEvent.Post event) {
        AuthorisedKeysModClient.tick();
    }
}
