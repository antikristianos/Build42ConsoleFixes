package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.vehicles.BaseVehicle;

@Patch(className = "zombie.vehicles.BaseVehicle", methodName = "tryStartEngine",
        warmUp = true, strictMatch = true)
public final class PatchVehicleStartAuthorization {
    private PatchVehicleStartAuthorization() { }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.This BaseVehicle vehicle, @Patch.Argument(0) boolean haveKey) {
        return VehicleStartAuthorization.reject(vehicle, haveKey);
    }
}
