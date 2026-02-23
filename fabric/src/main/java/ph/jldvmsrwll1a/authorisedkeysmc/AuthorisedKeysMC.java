package ph.jldvmsrwll1a.authorisedkeysmc;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import ph.jldvmsrwll1a.authorisedkeysmc.command.ModCommands;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.FabricPlatformHelper;

public class AuthorisedKeysMC implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        AkmcCore.init(new FabricPlatformHelper());

        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    private void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext context,
            Commands.CommandSelection environment) {
        ModCommands.register(dispatcher, context, environment);
    }
}
