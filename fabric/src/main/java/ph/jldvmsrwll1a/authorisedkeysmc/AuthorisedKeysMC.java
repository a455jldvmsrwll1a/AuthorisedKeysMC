package ph.jldvmsrwll1a.authorisedkeysmc;

import net.fabricmc.api.ModInitializer;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.FabricPlatformHelper;

public class AuthorisedKeysMC implements ModInitializer {
    
    @Override
    public void onInitialize() {
        
        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.

        // Use Fabric to bootstrap the Common mod.
        Constants.LOG.info("Hello Fabric world!");
        CommonClass.init(new FabricPlatformHelper());
    }
}
