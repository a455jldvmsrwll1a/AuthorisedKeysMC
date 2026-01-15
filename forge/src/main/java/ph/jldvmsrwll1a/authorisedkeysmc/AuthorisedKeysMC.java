package ph.jldvmsrwll1a.authorisedkeysmc;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.ForgePlatformHelper;

@Mod(Constants.MOD_ID)
public class AuthorisedKeysMC {
    public AuthorisedKeysMC() {
        AuthorisedKeysModCore.init(new ForgePlatformHelper());

        if (FMLLoader.getDist().isClient()) {
            AuthorisedKeysModClient.init();
        }
    }
}
