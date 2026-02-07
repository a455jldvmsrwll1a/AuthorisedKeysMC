package ph.jldvmsrwll1a.authorisedkeysmc.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = Version.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (input != null) {
                PROPERTIES.load(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getProjectVersion() {
        return PROPERTIES.getProperty("project.version", "");
    }
}
