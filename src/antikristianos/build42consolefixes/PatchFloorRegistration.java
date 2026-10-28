package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.entity.GameEntity;

@Patch(className = "zombie.entity.GameEntityManager", methodName = "RegisterEntity", warmUp = true, strictMatch = true)
public final class PatchFloorRegistration {
    private PatchFloorRegistration() { }
    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.Argument(0) GameEntity entity) {
        return FloorPlacement.deferRegistration(entity);
    }
}
