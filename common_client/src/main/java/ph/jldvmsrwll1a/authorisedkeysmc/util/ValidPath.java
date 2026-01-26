package ph.jldvmsrwll1a.authorisedkeysmc.util;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public final class ValidPath {
    public static final int MAX_LENGTH = 64;

    private static final Set<String> RESERVED = Set.of(
            "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1",
            "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    public static Path makePath(@Nullable Path base, @NotNull String name, @Nullable String extension) {
        Validate.notNull(name, "must supply a name");

        if (base == null) {
            base = Path.of(".");
        }

        if (extension == null) {
            extension = "";
        }

        if (name.isBlank() || name.length() > MAX_LENGTH) {
            throw new InvalidPathException(name, "invalid length");
        }

        int len = name.length();

        for (int i = 0; i < len; ++i) {
            char c = name.charAt(i);

            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == ' '
                    || c == '_'
                    || c == '-'
                    || c == '.'
                    || c == '+';

            if (!ok) {
                throw new InvalidPathException(name, "invalid character: %s".formatted(c));
            }
        }

        if (RESERVED.contains(name.toUpperCase(Locale.ROOT))) {
            throw new InvalidPathException(name, "filename may be reserved");
        }

        base = base.toAbsolutePath().normalize();
        Path file = base.resolve(name + extension).normalize();

        if (!file.startsWith(base)) {
            throw new InvalidPathException(name, "path is not within base directory");
        }

        return file;
    }
}
