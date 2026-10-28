package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.ZomboidFileSystem", methodName = "validatePrefix",
        warmUp = true, strictMatch = true)
public final class PatchValidatePrefix {
    private PatchValidatePrefix() {
    }

    @Patch.OnEnter
    public static void enter(@Patch.Argument(0) String path) {
        ModFolderScanGuard.enter();
    }

    @Patch.OnExit(onThrowable = Throwable.class)
    public static void exit() {
        ModFolderScanGuard.exit();
    }
}
