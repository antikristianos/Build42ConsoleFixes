package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoGameCharacter;
import zombie.vehicles.BaseVehicle;

@Patch(className = "zombie.characters.IsoGameCharacter", methodName = "setVehicle",
        warmUp = true, strictMatch = true)
public final class PatchSeatedEquipmentTransition {
    private PatchSeatedEquipmentTransition() { }

    @Patch.OnEnter
    public static void enter(@Patch.This IsoGameCharacter character,
            @Patch.Argument(0) BaseVehicle vehicle, @Patch.Local("wasSeated") boolean wasSeated) {
        wasSeated = character.getVehicle() != null;
    }

    @Patch.OnExit
    public static void exit(@Patch.This IsoGameCharacter character,
            @Patch.Argument(0) BaseVehicle vehicle, @Patch.Local("wasSeated") boolean wasSeated) {
        SeatedEquipmentModels.vehicleChanged(character, wasSeated);
    }
}
