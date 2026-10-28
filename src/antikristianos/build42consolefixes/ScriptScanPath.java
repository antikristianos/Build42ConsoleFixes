package antikristianos.build42consolefixes;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/** Aligns a junction-backed scan path with Vanilla's canonical mod base URI. */
public final class ScriptScanPath {
    private ScriptScanPath() {
    }

    public static File alignWithBase(URI base, File path) {
        if (base == null || path == null || !"file".equalsIgnoreCase(base.getScheme())
                || base.getPath() == null || !base.getPath().endsWith("/")) {
            return path;
        }
        try {
            if (isWithinBase(base, path)) {
                return path;
            }
            File canonical = path.getCanonicalFile();
            return isWithinBase(base, canonical) ? canonical : path;
        } catch (IOException | SecurityException | IllegalArgumentException ignored) {
            // Preserve Vanilla's handling of inaccessible, invalid and unrelated paths.
            return path;
        }
    }

    private static boolean isWithinBase(URI base, File path) {
        URI uri = new File(path.getAbsolutePath().toLowerCase(Locale.ENGLISH)).toURI();
        return !base.relativize(uri).isAbsolute();
    }
}
