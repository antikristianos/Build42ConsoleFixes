package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.ui.ActionProgressBar;

@Patch(className = "zombie.ui.ActionProgressBar", methodName = "updateScreenPos", warmUp = true, strictMatch = true)
public final class PatchOverheadBarPosition {
    private PatchOverheadBarPosition() { }
    @Patch.OnExit
    public static void exit(@Patch.This ActionProgressBar bar, @Patch.Argument(0) int playerIndex) {
        OverheadDisplayScale.positionBar(bar, playerIndex);
    }
}
