package ph.jldvmsrwll1a.authorisedkeysmc;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public class LoadedKeypair {
    private final @NotNull String name;
    private final @NotNull Instant modificationTime;
    private final @Nullable PKCS8EncryptedPrivateKeyInfo encryptedInfo;

    private @Nullable Ed25519PublicKeyParameters publicKey;
    private @Nullable Ed25519PrivateKeyParameters privateKey;

    public LoadedKeypair(
            @NotNull String name,
            @NotNull Instant modificationTime,
            @NotNull Ed25519PrivateKeyParameters privateKey,
            @Nullable Ed25519PublicKeyParameters publicKey) {
        this.name = name;
        this.modificationTime = modificationTime;
        this.encryptedInfo = null;

        this.privateKey = privateKey;
        this.publicKey = publicKey != null ? publicKey : privateKey.generatePublicKey();
    }

    public LoadedKeypair(
            @NotNull String name,
            @NotNull Instant modificationTime,
            @NotNull PKCS8EncryptedPrivateKeyInfo encryptedInfo,
            @Nullable Ed25519PublicKeyParameters publicKey) {
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

    public @NotNull String getTextualPublic() {
        return Base64Util.encode(getPublic().getEncoded());
    }

    public @NotNull Ed25519PublicKeyParameters getPublic() {
        if (publicKey == null && privateKey != null) {
            publicKey = privateKey.generatePublicKey();
        }

        if (publicKey != null) {
            return publicKey;
        } else {
            throw new IllegalStateException(
                    "public key is undetermined (private key has likely not yet been decrypted, and no public key was explicitly supplied in the key file)");
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
            InputDecryptorProvider decryptorProvider =
                    new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password.toCharArray());
            PrivateKeyInfo pki = encryptedInfo.decryptPrivateKeyInfo(decryptorProvider);
            AsymmetricKeyParameter key = PrivateKeyFactory.createKey(pki);

            if (key instanceof Ed25519PrivateKeyParameters edPri) {
                privateKey = edPri;

                if (publicKey == null) {
                    publicKey = edPri.generatePublicKey();
                }

                return true;
            } else {
                throw new IllegalArgumentException("Expected a %s but found a %s!"
                        .formatted(
                                Ed25519PublicKeyParameters.class.getName(),
                                key.getClass().getName()));
            }
        } catch (OperatorCreationException | IOException e) {
            throw new RuntimeException(e);
        } catch (PKCSException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return getTextualPublic();
    }

    public static LoadedKeypair fromFile(@NotNull Path path, @NotNull String name)
            throws IOException, InvalidPathException {
        Instant modificationTime = Files.getLastModifiedTime(path).toInstant();

        Ed25519PrivateKeyParameters privateKey = null;
        Ed25519PublicKeyParameters publicKey = null;
        PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = null;

        try (PemReader reader = new PemReader(new FileReader(path.toFile()))) {
            while (true) {
                PemObject pem = reader.readPemObject();

                if (pem == null) {
                    break;
                }

                switch (pem.getType()) {
                    case "PRIVATE KEY" -> {
                        AsymmetricKeyParameter key = PrivateKeyFactory.createKey(pem.getContent());

                        if (key instanceof Ed25519PrivateKeyParameters edPri) {
                            privateKey = edPri;
                        } else {
                            throw new IllegalArgumentException("expected a private key of type %s but found a %s"
                                    .formatted(
                                            Ed25519PrivateKeyParameters.class.getName(),
                                            key.getClass().getName()));
                        }
                    }
                    case "ENCRYPTED PRIVATE KEY" ->
                        encryptedPrivateKeyInfo = new PKCS8EncryptedPrivateKeyInfo(pem.getContent());
                    case "PUBLIC KEY" -> {
                        AsymmetricKeyParameter key = PublicKeyFactory.createKey(pem.getContent());

                        if (key instanceof Ed25519PublicKeyParameters edPub) {
                            publicKey = edPub;
                        } else {
                            throw new IllegalArgumentException("expected a public key of type %s but found a %s"
                                    .formatted(
                                            Ed25519PublicKeyParameters.class.getName(),
                                            key.getClass().getName()));
                        }
                    }
                }
            }
        }

        if (privateKey != null) {
            return new LoadedKeypair(name, modificationTime, privateKey, publicKey);
        } else if (encryptedPrivateKeyInfo != null) {
            return new LoadedKeypair(name, modificationTime, encryptedPrivateKeyInfo, publicKey);
        } else {
            throw new IllegalArgumentException("File contains no valid data.");
        }
    }
}
