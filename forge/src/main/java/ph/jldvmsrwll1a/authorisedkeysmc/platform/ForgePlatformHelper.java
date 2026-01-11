package ph.jldvmsrwll1a.authorisedkeysmc.platform;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }

    @Override
    public Path getGameDirectory() {
        return FMLLoader.getGamePath();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLLoader.getGamePath().resolve("config");
    }
}