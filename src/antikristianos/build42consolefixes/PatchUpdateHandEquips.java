package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoGameCharacter;

@Patch(className = "zombie.characters.IsoGameCharacter", methodName = "updateHandEquips",
        warmUp = true, strictMatch = true)
public final class PatchUpdateHandEquips {
    private PatchUpdateHandEquips() { }
    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.This IsoGameCharacter character) {
        return HandItemDrop.updateNpcHands(character);
    }
}
