package ph.jldvmsrwll1a.authorisedkeysmc;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.ForgePlatformHelper;

@Mod(Constants.MOD_ID)
public class AuthorisedKeysMC {
    public AuthorisedKeysMC() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(this::onTick);

        AuthorisedKeysModCore.init(new ForgePlatformHelper());

        if (FMLLoader.getDist().isClient()) {
            AuthorisedKeysModClient.init();
        }
    }

    private void onTick(TickEvent.ClientTickEvent.Post event) {
        AuthorisedKeysModClient.tick();
    }
}
