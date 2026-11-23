package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.skinnedmodel.model.ModelInstance;

@Patch(className = "zombie.core.skinnedmodel.ModelManager", methodName = "addStaticForcedTex",
        warmUp = true, strictMatch = true)
public final class PatchSeatedBackWeapon {
    private PatchSeatedBackWeapon() { }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.Argument(0) ModelInstance parent,
            @Patch.Argument(1) String model, @Patch.Argument(2) String selfAttachment,
            @Patch.Argument(3) String parentAttachment, @Patch.Argument(4) String texture) {
        return SeatedEquipmentModels.attachedItem(parent, model, parentAttachment);
    }
}
