package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.util.Arrays;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public final class AkPublicKey {
    /// When encoded through base64url.
    public static final int STRING_LENGTH = 43;
    public static final int ENCODED_LENGTH = Ed25519PublicKeyParameters.KEY_SIZE;

    private final Ed25519PublicKeyParameters parameters;
    private final byte[] encodedBytes;
    private final int hash;

    public AkPublicKey(AkPrivateKey privateKey) {
        parameters = privateKey.getInternal().generatePublicKey();
        encodedBytes = parameters.getEncoded();
        hash = Arrays.hashCode(encodedBytes);
    }

    public AkPublicKey(String encoded) {
        Validate.isTrue(encoded.length() == STRING_LENGTH, "Invalid public key string length.");

        byte[] bytes = Base64Util.decode(encoded);
        parameters = new Ed25519PublicKeyParameters(bytes);
        encodedBytes = bytes;
        hash = Arrays.hashCode(bytes);
    }

    public AkPublicKey(byte[] bytes) {
        parameters = new Ed25519PublicKeyParameters(bytes);
        encodedBytes = bytes;
        hash = Arrays.hashCode(bytes);
    }

    public static boolean nullableEqual(@Nullable AkPublicKey a, @Nullable AkPublicKey b) {
        if (a == null) {
            return b != null;
        } else {
            return a.equals(b);
        }
    }

    public byte[] getEncoded() {
        return encodedBytes;
    }

    @Override
    public String toString() {
        return Base64Util.encode(encodedBytes);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof AkPublicKey key) {
            return hash == key.hash && Arrays.equals(encodedBytes, key.encodedBytes);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return hash;
    }

    Ed25519PublicKeyParameters getInternal() {
        return parameters;
    }
}
