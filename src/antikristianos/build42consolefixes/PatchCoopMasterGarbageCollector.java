package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(
        className = "zombie.network.CoopMaster",
        methodName = "getGarbageCollector",
        warmUp = true,
        strictMatch = true)
public final class PatchCoopMasterGarbageCollector {
    private PatchCoopMasterGarbageCollector() {
    }

    @Patch.OnExit
    public static void exit(@Patch.Return(readOnly = false) String argument) {
        argument = CoopWorkshopLaunch.arguments(argument);
    }
}
