package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.ZomboidFileSystem", methodName = "resetModFolders",
        warmUp = true, strictMatch = true)
public final class PatchResetModFolders {
    private PatchResetModFolders() {
    }

    @Patch.OnEnter
    public static void enter() {
        ModFolderScanGuard.enter();
    }

    @Patch.OnExit(onThrowable = Throwable.class)
    public static void exit() {
        ModFolderScanGuard.exit();
    }
}
