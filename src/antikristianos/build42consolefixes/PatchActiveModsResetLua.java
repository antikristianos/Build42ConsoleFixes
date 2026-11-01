package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.modding.ActiveMods;

@Patch(className = "zombie.modding.ActiveMods", methodName = "requiresResetLua",
        warmUp = true, strictMatch = true)
public final class PatchActiveModsResetLua {
    private PatchActiveModsResetLua() { }

    @Patch.OnExit
    public static void exit(@Patch.Argument(0) ActiveMods requested,
            @Patch.Return(readOnly = false) boolean returned) {
        if (returned && ActiveModSelection.sameLoadedSelection(requested)) returned = false;
    }
}
