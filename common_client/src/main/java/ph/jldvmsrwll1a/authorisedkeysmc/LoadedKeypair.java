package ph.jldvmsrwll1a.authorisedkeysmc;

import org.apache.commons.lang3.Validate;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;

public class LoadedKeypair {
    private final @NotNull String name;
    private final @NotNull Instant modificationTime;
    private final @Nullable PKCS8EncryptedPrivateKeyInfo encryptedInfo;

    private @Nullable Ed25519PublicKeyParameters publicKey;
    private @Nullable Ed25519PrivateKeyParameters privateKey;

    public LoadedKeypair(@NotNull String name, @NotNull Instant modificationTime, @NotNull Ed25519PrivateKeyParameters privateKey, @Nullable Ed25519PublicKeyParameters publicKey) {
        this.name = name;
        this.modificationTime = modificationTime;
        this.encryptedInfo = null;

        this.privateKey = privateKey;
        this.publicKey = publicKey != null ? publicKey : privateKey.generatePublicKey();
    }

    public LoadedKeypair(@NotNull String name, @NotNull Instant modificationTime, @NotNull PKCS8EncryptedPrivateKeyInfo encryptedInfo, @Nullable Ed25519PublicKeyParameters publicKey) {
        this.name = name;
        this.modificationTime = modificationTime;
        this.encryptedInfo = encryptedInfo;

        this.privateKey = null;
        this.publicKey = publicKey;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull Instant getModificationTime() {
        return modificationTime;
    }

    public @NotNull Ed25519PublicKeyParameters getPublic() {
        if (publicKey == null && privateKey != null) {
            publicKey = privateKey.generatePublicKey();
        }

        if (publicKey != null) {
            return publicKey;
        } else {
            throw new IllegalStateException("public key is undetermined (private key has likely not yet been decrypted, and no public key was explicitly supplied in the key file)");
        }
    }

    public @NotNull Ed25519PrivateKeyParameters getDecryptedPrivate() {
        if (privateKey != null) {
            return privateKey;
        } else if (encryptedInfo != null) {
            throw new IllegalStateException("private key has likely not yet been decrypted");
        } else {
            throw new IllegalStateException("Degenerate keypair does not contain any data.");
        }
    }

    public boolean requiresDecryption() {
        return privateKey == null && encryptedInfo != null;
    }

    public boolean decrypt(@NotNull String password) {
        Validate.validState(encryptedInfo != null, "Not an encrypted private key.");

        try {
            InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password.toCharArray());
            PrivateKeyInfo pki = encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
            AsymmetricKeyParameter key = PrivateKeyFactory.createKey(pki);

            if (key instanceof Ed25519PrivateKeyParameters edPri) {
                privateKey = edPri;

                if (publicKey == null) {
                    publicKey = edPri.generatePublicKey();
                }

                return true;
            } else {
                throw new IllegalArgumentException("Expected a %s but found a %s!".formatted(Ed25519PublicKeyParameters.class.getName(), key.getClass().getName()));
            }
        } catch (OperatorCreationException | IOException e) {
            throw new RuntimeException(e);
        } catch (PKCSException e) {
            return false;
        }
    }
}
