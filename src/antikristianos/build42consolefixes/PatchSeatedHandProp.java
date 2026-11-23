package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.skinnedmodel.ModelManager.ModelSlot;

@Patch(className = "zombie.core.skinnedmodel.ModelManager", methodName = "addStatic",
        warmUp = true, strictMatch = true)
public final class PatchSeatedHandProp {
    private PatchSeatedHandProp() { }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.AllArguments Object[] arguments) {
        // Buddy can match the longer addStatic overload sharing these first three types.
        // Gate arity here and avoid indexed casts on the other native overloads.
        return arguments.length == 3 && arguments[0] instanceof ModelSlot slot
                && arguments[2] instanceof String bone
                && SeatedEquipmentModels.handProp(slot.character, bone);
    }
}
