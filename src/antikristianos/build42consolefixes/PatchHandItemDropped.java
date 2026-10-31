package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.network.packets.character.PlayerDropHeldItemsPacket;
import zombie.util.flags.EnumByteFlags;

@Patch(className = "zombie.characters.IsoGameCharacter", methodName = "onDroppedHandItem",
        warmUp = true, strictMatch = true)
public final class PatchHandItemDropped {
    private PatchHandItemDropped() { }
    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.This IsoGameCharacter character,
            @Patch.Argument(0) IsoGridSquare square, @Patch.Argument(1) InventoryItem item,
            @Patch.Argument(2) EnumByteFlags<PlayerDropHeldItemsPacket.Flag> flags) {
        return HandItemDrop.finishNpcDrop(character, square, item, flags);
    }
}
