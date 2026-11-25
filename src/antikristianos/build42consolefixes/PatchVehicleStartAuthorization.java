package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.inventory.types.DrainableComboItem;
import zombie.network.GameServer;

@Patch(className = "zombie.vehicles.BaseVehicle", methodName = "tryStartEngine",
        warmUp = true, strictMatch = true)
public final class PatchVehicleStartAuthorization {
    private PatchVehicleStartAuthorization() { }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.This BaseVehicle vehicle, @Patch.Argument(0) boolean haveKey,
            @Patch.Local("starterBattery") VehiclePart battery,
            @Patch.Local("starterCharge") float charge) {
        if (VehicleStartAuthorization.reject(vehicle, haveKey)) return true;
        if (GameServer.server) {
            VehiclePart part = vehicle.getBattery();
            if (part != null && part.getInventoryItem() instanceof DrainableComboItem) {
                battery = part;
                charge = part.getInventoryItem().getCurrentUsesFloat();
            }
        }
        return false;
    }

    @Patch.OnExit
    public static void exit(@Patch.This BaseVehicle vehicle,
            @Patch.Argument(0) boolean haveKey,
            @Patch.Local("starterBattery") VehiclePart battery,
            @Patch.Local("starterCharge") float charge) {
        if (GameServer.server && battery != null
                && battery.getInventoryItem() instanceof DrainableComboItem
                && battery.getInventoryItem().getCurrentUsesFloat() != charge) {
            vehicle.transmitPartUsedDelta(battery);
        }
    }
}
