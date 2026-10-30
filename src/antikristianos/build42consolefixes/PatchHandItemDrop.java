package antikristianos.build42consolefixes;

import java.util.function.Predicate;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoGridSquare;

@Patch(className = "zombie.characters.IsoGameCharacter", methodName = "tryPerformDropHeldItem",
        warmUp = true, strictMatch = true)
public final class PatchHandItemDrop {
    private PatchHandItemDrop() { }
    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.This IsoGameCharacter character,
            @Patch.Argument(0) Predicate<IsoGridSquare> performDrop,
            @Patch.Local("handDropResult") boolean result) {
        result = HandItemDrop.perform(character, performDrop);
        return true;
    }
    @Patch.OnExit
    public static void exit(@Patch.Local("handDropResult") boolean result,
            @Patch.Return(readOnly = false) boolean returned) {
        returned = result;
    }
}
