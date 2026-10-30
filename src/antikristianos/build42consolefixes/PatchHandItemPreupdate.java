package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoGameCharacter;

@Patch(className = "zombie.characters.IsoGameCharacter", methodName = "preupdate",
        warmUp = true, strictMatch = true)
public final class PatchHandItemPreupdate {
    private PatchHandItemPreupdate() { }
    @Patch.OnEnter
    public static void enter(@Patch.This IsoGameCharacter character) {
        HandItemDrop.preparePreupdate(character);
    }
}
