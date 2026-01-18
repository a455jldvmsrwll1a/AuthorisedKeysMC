package ph.jldvmsrwll1a.authorisedkeysmc.util;

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public abstract class KeyUtil {
    private KeyUtil() {

    }

    public static boolean areNullableKeysEqual(@Nullable Ed25519PublicKeyParameters a, @Nullable Ed25519PublicKeyParameters b) {
        if (a == b) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        return Arrays.equals(a.getEncoded(), b.getEncoded());
    }
}
