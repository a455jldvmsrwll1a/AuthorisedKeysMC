package ph.jldvmsrwll1a.authorisedkeysmc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Constants {
	public static final String MOD_ID = "authorisedkeysmc";
    public static final String MOD_DIR_NAME = "authorised-keys-mc";
	public static final String MOD_NAME = "AuthorisedKeysMC";

    public static final int PUBKEY_SIZE = 44;
    public static final String ALGORITHM = "Ed25519";

    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
}