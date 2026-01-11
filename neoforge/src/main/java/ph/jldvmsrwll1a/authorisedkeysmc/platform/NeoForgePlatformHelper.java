package ph.jldvmsrwll1a.authorisedkeysmc.platform;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public Path getGameDirectory() {
        return FMLLoader.getCurrent().getGameDir();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLLoader.getCurrent().getGameDir().resolve("config");
    }
}