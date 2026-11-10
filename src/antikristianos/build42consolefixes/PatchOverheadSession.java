package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.gameStates.GameLoadingState", methodName = "enter", warmUp = true, strictMatch = true)
public final class PatchOverheadSession {
    private PatchOverheadSession() { }
    @Patch.OnEnter
    public static void enter() { OverheadDisplayScale.startSession(); }
}
