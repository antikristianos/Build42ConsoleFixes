package antikristianos.build42consolefixes;

import java.nio.file.Path;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import me.zed_0xff.zombie_buddy.Patch;

@Patch(
        className = "zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator",
        methodName = "searchFolders",
        warmUp = true,
        strictMatch = true)
public final class PatchAdvancedAnimatorSearchFolders {
    private PatchAdvancedAnimatorSearchFolders() {
    }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(
            @Patch.Argument(0) URI base,
            @Patch.Argument(value = 1, readOnly = false) Path root,
            @Patch.Local("missingOptionalRoot") boolean missingOptionalRoot) {
        if (root != null) root = ScriptScanPath.alignWithBase(base, root.toFile()).toPath();
        missingOptionalRoot = OptionalDirectoryScan.shouldReturnEmpty(root);
        return missingOptionalRoot;
    }

    @Patch.OnExit
    public static void exit(
            @Patch.Local("missingOptionalRoot") boolean missingOptionalRoot,
            @Patch.Return(readOnly = false) List<String> result) {
        if (missingOptionalRoot) {
            result = new ArrayList<>();
        }
    }
}
