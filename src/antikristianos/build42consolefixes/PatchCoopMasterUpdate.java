package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.network.CoopMaster", methodName = "update", warmUp = true, strictMatch = true)
public final class PatchCoopMasterUpdate {
    private PatchCoopMasterUpdate() {}
    @Patch.OnExit
    public static void exit() { CoopWorkshopClient.update(); }
}