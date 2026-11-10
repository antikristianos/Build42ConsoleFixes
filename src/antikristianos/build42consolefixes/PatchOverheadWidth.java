package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.ui.TextDrawObject;

@Patch(className = "zombie.ui.TextDrawObject", methodName = "getWidth", warmUp = true, strictMatch = true)
public final class PatchOverheadWidth {
    private PatchOverheadWidth() { }
    @Patch.OnExit
    public static void exit(@Patch.This TextDrawObject text, @Patch.Return(readOnly = false) int result) {
        result = OverheadDisplayScale.metric(text, result, true);
    }
}
