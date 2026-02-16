package ph.jldvmsrwll1a.authorisedkeysmc;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import ph.jldvmsrwll1a.authorisedkeysmc.command.ModCommands;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.ForgePlatformHelper;

@Mod(Constants.MOD_ID)
public class AuthorisedKeysMC {
    public AuthorisedKeysMC(FMLJavaModLoadingContext context) {
        TickEvent.ClientTickEvent.Post.BUS.addListener(this::onTick);
        RegisterCommandsEvent.BUS.addListener(this::onCommandRegistration);

        AkmcCore.init(new ForgePlatformHelper());

        if (FMLEnvironment.dist.isClient()) {
            new AuthorisedKeysMCClient(context);
        }
    }

    private void onTick(TickEvent.ClientTickEvent.Post event) {
        AkmcClient.tick();
    }

    private void onCommandRegistration(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }
}
