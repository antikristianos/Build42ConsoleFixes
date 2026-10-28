package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.iso.IsoObject;

@Patch(className = "zombie.iso.IsoObject", methodName = "transmitCompleteItemToClients", warmUp = true, strictMatch = true)
public final class PatchFloorTransmit {
    private PatchFloorTransmit() { }
    @Patch.OnEnter
    public static void enter(@Patch.This IsoObject object) { FloorPlacement.beforeTransmit(object); }
}
