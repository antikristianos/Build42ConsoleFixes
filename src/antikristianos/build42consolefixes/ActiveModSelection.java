package antikristianos.build42consolefixes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.modding.ActiveMods;

/** Compares the native loaded snapshot without modifying either mod list. */
public final class ActiveModSelection {
    private static final ActiveMods LOADED = resolveLoadedMods();

    private ActiveModSelection() { }

    private static ActiveMods resolveLoadedMods() {
        try {
            Field field = ActiveMods.class.getDeclaredField("s_loaded");
            if (field.getType() != ActiveMods.class || !Modifier.isStatic(field.getModifiers())
                    || !Modifier.isFinal(field.getModifiers())) {
                return null;
            }
            field.setAccessible(true);
            return (ActiveMods) field.get(null);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            // An unsupported runtime keeps Vanilla's reload decision.
            return null;
        }
    }

    public static boolean sameLoadedSelection(ActiveMods requested) {
        if (LOADED == null || requested == null) return false;
        List<String> loaded = LOADED.getMods();
        List<String> selected = requested.getMods();
        if (loaded.size() != selected.size()) return false;
        // Preserve multiplicity and exact IDs; ignore only order.
        Map<String, Integer> counts = new HashMap<>();
        for (String id : loaded) counts.merge(id, 1, Integer::sum);
        for (String id : selected) {
            Integer count = counts.get(id);
            if (count == null) return false;
            if (count == 1) counts.remove(id);
            else counts.put(id, count - 1);
        }
        return counts.isEmpty();
    }
}
