package antikristianos.build42consolefixes;

import java.util.List;
import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.ZomboidFileSystem", methodName = "getAllModFolders",
        warmUp = true, strictMatch = true)
public final class PatchModFolderSnapshot {
    private PatchModFolderSnapshot() {
    }

    @Patch.OnEnter
    public static void enter(@Patch.Argument(0) List<String> folders) {
        ModFolderScanGuard.enter();
    }

    @Patch.OnExit(onThrowable = Throwable.class)
    public static void exit() {
        ModFolderScanGuard.exit();
    }
}
