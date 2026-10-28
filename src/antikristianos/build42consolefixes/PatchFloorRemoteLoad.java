package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.network.ByteBufferReader;
import zombie.iso.IsoObject;

@Patch(className = "zombie.iso.IsoObject", methodName = "loadFromRemoteBuffer", warmUp = true, strictMatch = true)
public final class PatchFloorRemoteLoad {
    private PatchFloorRemoteLoad() { }
    @Patch.OnExit
    public static void exit(@Patch.This IsoObject object, @Patch.Argument(0) ByteBufferReader reader,
                            @Patch.Argument(1) boolean addToObjects) {
        FloorPlacement.afterRemoteLoad(object, addToObjects);
    }
}
