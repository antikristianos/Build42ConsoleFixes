package antikristianos.build42consolefixes;

import java.nio.file.Files;
import java.nio.file.Path;

/** Identifies an optional scan root that is conclusively absent. */
public final class OptionalDirectoryScan {
    private OptionalDirectoryScan() {
    }

    public static boolean shouldReturnEmpty(Path requested) {
        return requested != null && Files.notExists(requested);
    }
}
