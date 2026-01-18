package ph.jldvmsrwll1a.authorisedkeysmc;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Constants {
	public static final String MOD_ID = "authorisedkeysmc";
    public static final String MOD_DIR_NAME = "authorised_keys_mc";
	public static final String MOD_NAME = "AuthorisedKeysMC";

    public static final Identifier LOGIN_CHANNEL_ID = Identifier.fromNamespaceAndPath("authorised_keys_mc", "login");

    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
}