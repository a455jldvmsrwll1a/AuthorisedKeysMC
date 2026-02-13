package ph.jldvmsrwll1a.authorisedkeysmc;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.PortalScreen;

@Environment(EnvType.CLIENT)
public final class ModMenuIntegration implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PortalScreen::new;
    }
}
