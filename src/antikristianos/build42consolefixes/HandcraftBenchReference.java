package antikristianos.build42consolefixes;

import java.lang.reflect.Field;
import zombie.characters.IsoPlayer;
import zombie.core.NetTimedAction;
import zombie.core.network.ByteBufferReader;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.crafting.recipe.HandcraftLogic;
import zombie.iso.IsoObject;
import zombie.network.GameServer;
import zombie.network.PZNetKahluaTableImpl;
import zombie.scripting.entity.components.crafting.CraftBenchScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;

/** Resolves only a handcraft request's redundant bench reference, never the entity registry. */
public final class HandcraftBenchReference {
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
    private static final Field ACTION_ARGS = actionArgsField();
    private static final byte CRAFT_BENCH_VALUE = 36;

    private HandcraftBenchReference() { }

    private static Field actionArgsField() {
        try {
            Field field = NetTimedAction.class.getDeclaredField("actionArgs");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static Scope enter(NetTimedAction action) {
        Scope scope = new Scope(action, CURRENT.get());
        CURRENT.set(scope);
        return scope;
    }

    public static boolean handles(PZNetKahluaTableImpl table, byte valueType, int depth) {
        Scope scope = CURRENT.get();
        if (!GameServer.server || scope == null || valueType != CRAFT_BENCH_VALUE || depth != 0
                || !"ISHandcraftAction".equals(scope.action.type)) {
            return false;
        }
        try {
            return ACTION_ARGS.get(scope.action) == table;
        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);
        }
    }

    public static Component read(PZNetKahluaTableImpl table, ByteBufferReader input) {
        Scope scope = CURRENT.get();
        // Retain the native wire layout. The world object, not this redundant ID, is authoritative.
        input.getLong();
        short componentType = input.getShort();
        scope.benchValues++;
        Object object = table.rawget("isoObject");
        if (componentType != ComponentType.CraftBench.GetID() || !(object instanceof IsoObject)) {
            scope.invalid = true;
            return null;
        }
        scope.object = (IsoObject) object;
        scope.bench = scope.object.getComponent(ComponentType.CraftBench);
        if (scope.bench == null || scope.bench.getOwner() != scope.object) {
            scope.invalid = true;
        }
        return scope.bench;
    }

    public static void exit(Scope scope) {
        try {
            if (scope.benchValues != 0 && (scope.invalid || !validWorkplace(scope))) {
                // Native NetTimedActionPacket.processServer sends Reject for a null action.
                // Do not turn an invalid bench request into unrestricted handcrafting.
                scope.action.action = null;
            }
        } finally {
            if (scope.previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(scope.previous);
            }
        }
    }

    private static boolean validWorkplace(Scope scope) {
        if (scope.benchValues != 1 || scope.action.action == null || scope.object == null
                || scope.object.getSquare() == null || scope.object.getObjectIndex() < 0
                || scope.action.action.rawget("isoObject") != scope.object
                || scope.action.action.rawget("craftBench") != scope.bench
                || !(scope.action.action.rawget("character") instanceof IsoPlayer player)
                || !(scope.action.action.rawget("craftRecipe") instanceof CraftRecipe recipe)
                || !scope.object.isUsingPlayer(player) || scope.object.getEntityScript() == null) {
            return false;
        }
        CraftBenchScript script = (CraftBenchScript) scope.object.getEntityScript()
                .getComponentScriptFor(ComponentType.CraftBench);
        if (script == null || !script.getRecipes().contains(recipe)) {
            return false;
        }
        HandcraftLogic logic = new HandcraftLogic(player, scope.bench, scope.object);
        logic.setRecipe(recipe);
        return logic.isCharacterInRangeOfWorkbench() && logic.hasRequiredWorkstation();
    }

    /** Public because ZombieBuddy inlines advice into Vanilla packages. */
    public static final class Scope {
        private final NetTimedAction action;
        private final Scope previous;
        private IsoObject object;
        private CraftBench bench;
        private int benchValues;
        private boolean invalid;

        private Scope(NetTimedAction action, Scope previous) {
            this.action = action;
            this.previous = previous;
        }
    }
}
