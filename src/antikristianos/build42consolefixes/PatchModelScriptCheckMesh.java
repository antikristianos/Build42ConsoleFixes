package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.network.GameServer;

@Patch(
        className = "zombie.scripting.objects.ModelScript",
        methodName = "checkMesh",
        warmUp = true,
        strictMatch = true,
        IKnowWhatIAmDoing = true)
public final class PatchModelScriptCheckMesh {
    private PatchModelScriptCheckMesh() {
    }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(
            @Patch.Argument(0) String owner,
            @Patch.Argument(1) String mesh) {
        return GameServer.server;
    }
}
