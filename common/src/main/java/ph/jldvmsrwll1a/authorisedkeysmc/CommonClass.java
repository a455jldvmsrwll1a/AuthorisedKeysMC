package ph.jldvmsrwll1a.authorisedkeysmc;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.IPlatformHelper;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class CommonClass {
    public static IPlatformHelper PLATFORM;
    public static FilePaths FILE_PATHS;

    // The loader specific projects are able to import and use any code from the common project. This allows you to
    // write the majority of your code here and load it from your loader specific projects. This example has some
    // code that gets invoked by the entry point of the loader specific projects.
    public static void init(@NotNull IPlatformHelper platform) {
        PLATFORM = platform;
        FILE_PATHS = new FilePaths(platform);

        Constants.LOG.info("Hello from Common init on {}! we are currently in a {} environment!", PLATFORM.getPlatformName(), PLATFORM.getEnvironmentName());
        Constants.LOG.info("The ID for diamonds is {}", BuiltInRegistries.ITEM.getKey(Items.DIAMOND));

        Constants.LOG.info("MOD_DIR: {}", FILE_PATHS.MOD_DIR);
        Constants.LOG.info("CONFIG_DIR: {}", FILE_PATHS.CONFIG_DIR);
        Constants.LOG.info("CLIENT_CONFIG_PATH: {}", FILE_PATHS.CLIENT_CONFIG_PATH);
        Constants.LOG.info("SERVER_CONFIG_PATH: {}", FILE_PATHS.SERVER_CONFIG_PATH);
        Constants.LOG.info("KEY_PAIRS_DIR: {}", FILE_PATHS.KEY_PAIRS_DIR);
        Constants.LOG.info("AUTHORISED_KEYS_PATH: {}", FILE_PATHS.AUTHORISED_KEYS_PATH);
        Constants.LOG.info("BYPASS_LIST_PATH: {}", FILE_PATHS.BYPASS_LIST_PATH);
        Constants.LOG.info("UUID_REMAPS_PATH: {}", FILE_PATHS.UUID_REMAPS_PATH);
        Constants.LOG.info("HISTORY_PATH: {}", FILE_PATHS.HISTORY_PATH);
        Constants.LOG.info("SERVER_KEY_PAIR_PATH: {}", FILE_PATHS.SERVER_KEY_PAIR_PATH);
    }
}