package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.inventory.InventoryItem;
import zombie.iso.objects.IsoCarBatteryCharger;

@Patch(className = "zombie.iso.objects.IsoCarBatteryCharger", methodName = "update", warmUp = true, strictMatch = true)
public final class PatchBatteryChargerUpdate {
    private PatchBatteryChargerUpdate() { }
    @Patch.OnEnter
    public static void enter(@Patch.This IsoCarBatteryCharger charger,
            @Patch.Local("chargingBattery") InventoryItem battery,
            @Patch.Local("chargingUses") int uses, @Patch.Local("chargingTime") float time) {
        battery = charger.getBattery();
        uses = battery == null ? 0 : battery.getCurrentUses();
        time = BatteryChargerRemainder.lastUpdate(charger);
    }
    @Patch.OnExit
    public static void exit(@Patch.This IsoCarBatteryCharger charger,
            @Patch.Local("chargingBattery") InventoryItem battery,
            @Patch.Local("chargingUses") int uses, @Patch.Local("chargingTime") float time) {
        BatteryChargerRemainder.finish(charger, battery, uses, time);
    }
}
