package ph.jldvmsrwll1a.authorisedkeysmc.util;

import java.util.Base64;

public final class Base64Util {
    private Base64Util() {}

    /// Encode bytes as URL-safe base64 but without trailing '='.
    public static String encode(byte[] buf) {
        String encoded = Base64.getUrlEncoder().encodeToString(buf);
        return encoded.substring(0, encoded.indexOf('='));
    }

    /// Decodes base64.
    public static byte[] decode(String encoded) throws IllegalArgumentException {
        return Base64.getUrlDecoder().decode(encoded);
    }
}
