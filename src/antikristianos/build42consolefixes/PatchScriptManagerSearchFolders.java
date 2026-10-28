package antikristianos.build42consolefixes;

import java.io.File;
import java.net.URI;
import me.zed_0xff.zombie_buddy.Patch;

@Patch(
        className = "zombie.scripting.ScriptManager",
        methodName = "searchFolders",
        warmUp = true,
        strictMatch = true)
public final class PatchScriptManagerSearchFolders {
    private PatchScriptManagerSearchFolders() {
    }

    @Patch.OnEnter
    public static void enter(
            @Patch.Argument(0) URI base,
            @Patch.Argument(value = 1, readOnly = false) File path) {
        path = ScriptScanPath.alignWithBase(base, path);
    }
}
