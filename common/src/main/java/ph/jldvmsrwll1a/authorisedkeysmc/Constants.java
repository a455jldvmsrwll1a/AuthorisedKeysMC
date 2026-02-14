package ph.jldvmsrwll1a.authorisedkeysmc;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
    public static final String MOD_ID = "authorisedkeysmc";
    public static final String MOD_DIR_NAME = "authorised_keys_mc";
    public static final String MOD_NAME = "AuthorisedKeysMC";

    public static final Identifier LOGIN_CHANNEL_ID = modId("login");

    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static final int PAYLOAD_HEADER = 0x414B4D43;
    public static final int PROTOCOL_VERSION = 100;

    public static final int KEY_PAIR_HEADER = PAYLOAD_HEADER;
    public static final short KEY_PAIR_VERSION = 1000;
    public static final String KEY_PAIR_EXTENSION = ".akmc";

    public static Identifier modId(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
