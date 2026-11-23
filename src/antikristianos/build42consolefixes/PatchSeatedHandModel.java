package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.ModelManager.ModelSlot;
import zombie.inventory.InventoryItem;
import zombie.scripting.objects.ItemReplacement;

@Patch(className = "zombie.core.skinnedmodel.ModelManager", methodName = "addEquippedModelInstance",
        warmUp = true, strictMatch = true)
public final class PatchSeatedHandModel {
    private PatchSeatedHandModel() { }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.Argument(0) IsoGameCharacter character,
            @Patch.Argument(1) ModelSlot slot, @Patch.Argument(2) InventoryItem item,
            @Patch.Argument(3) String bone, @Patch.Argument(4) ItemReplacement replacement,
            @Patch.Argument(5) boolean override) {
        return SeatedEquipmentModels.seated(character);
    }
}
