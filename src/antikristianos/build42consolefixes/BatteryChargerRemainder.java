package antikristianos.build42consolefixes;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import zombie.GameTime;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.iso.objects.IsoCarBatteryCharger;

/** Retains sub-use charging progress without changing the native update lifecycle. */
public final class BatteryChargerRemainder {
    private static final Field LAST_UPDATE;
    private static final Map<IsoCarBatteryCharger, Progress> PROGRESS = new WeakHashMap<>();
    static {
        try {
            LAST_UPDATE = IsoCarBatteryCharger.class.getDeclaredField("lastUpdate");
            LAST_UPDATE.setAccessible(true);
        } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private static final class Progress {
        WeakReference<InventoryItem> battery;
        int uses;
        float time;
        double fraction;
    }
    private BatteryChargerRemainder() { }

    public static float lastUpdate(IsoCarBatteryCharger charger) {
        try { return LAST_UPDATE.getFloat(charger); }
        catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
    }

    public static synchronized void finish(IsoCarBatteryCharger charger, InventoryItem battery,
            int beforeUses, float beforeTime) {
        float now = lastUpdate(charger);
        if (!(battery instanceof DrainableComboItem) || battery != charger.getBattery()
                || !charger.isActivated() || now < 0 || beforeTime < 0 || beforeTime > now
                || !Float.isFinite(now) || !Float.isFinite(beforeTime)) {
            PROGRESS.remove(charger); return;
        }
        int maximum = battery.getMaxUses();
        float rate = charger.getChargeRate();
        if (maximum <= 0 || beforeUses >= maximum || !(rate > 0) || !Float.isFinite(rate)) {
            PROGRESS.remove(charger); return;
        }
        Progress progress = PROGRESS.get(charger);
        if (progress != null && (progress.battery.get() != battery || progress.uses != beforeUses
                || progress.time != beforeTime)) {
            PROGRESS.remove(charger); progress = null;
        }
        float elapsed = now - GameTime.checkHours(beforeTime, now);
        if (!(elapsed > 0)) return;
        double units = Math.min(maximum, beforeUses + (double) maximum * rate * elapsed
                + (progress == null ? 0 : progress.fraction));
        int uses = Math.max(0, (int) Math.floor(units));
        if (battery.getCurrentUses() != uses) battery.setCurrentUses(uses);
        if (uses >= maximum) { PROGRESS.remove(charger); return; }
        if (progress == null) {
            progress = new Progress(); progress.battery = new WeakReference<>(battery);
            PROGRESS.put(charger, progress);
        }
        progress.uses = uses; progress.time = now; progress.fraction = units - uses;
    }
}
