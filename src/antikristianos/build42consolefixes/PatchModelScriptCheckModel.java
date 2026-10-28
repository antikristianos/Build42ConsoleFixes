package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.network.GameServer;

@Patch(
        className = "zombie.scripting.objects.ModelScript",
        methodName = "check",
        warmUp = true,
        strictMatch = true,
        IKnowWhatIAmDoing = true)
public final class PatchModelScriptCheckModel {
    private PatchModelScriptCheckModel() {
    }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(
            @Patch.Argument(0) String owner,
            @Patch.Argument(1) String model,
            @Patch.Argument(2) String clothingItem) {
        return GameServer.server;
    }
}
