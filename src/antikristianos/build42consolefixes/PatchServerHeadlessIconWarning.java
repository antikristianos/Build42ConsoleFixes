package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.network.GameServer;

@Patch(
        className = "zombie.debug.DebugType",
        methodName = "warn",
        warmUp = true,
        strictMatch = true)
public final class PatchServerHeadlessIconWarning {
    private PatchServerHeadlessIconWarning() {
    }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.Argument(0) Object message) {
        return HeadlessWarningFilter.shouldSuppress(GameServer.server, message);
    }
}
