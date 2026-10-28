package antikristianos.build42consolefixes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityManager;
import zombie.entity.util.LongMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.network.GameClient;

/** One synchronous native floor build. Never stored in a world object or a save. */
public final class FloorPlacement {
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();
    private static final Field NET_ID = field(IsoObject.class, "isoEntityNetId");
    private static final Field LAST_INDEX = field(IsoObject.class, "lastObjectIndex");
    private static final Field REGISTERED = field(GameEntity.class, "addedToEntityManager");
    private static final Field ENTITY_MAP = field(GameEntityManager.class, "idToEntityMap");
    private static final Method REGISTER = registrationMethod();

    private FloorPlacement() { }

    public static final class Scope {
        IsoGridSquare square;
        final Scope parent;
        final List<IsoObject> original;
        final IdentityHashMap<IsoObject, Long> oldIds = new IdentityHashMap<>();
        IsoObject floor;
        boolean finishing;
        boolean finished;

        Scope(IsoGridSquare square, Scope parent) {
            this.square = square;
            this.parent = parent;
            original = objects(square);
            for (IsoObject object : original) oldIds.put(object, cachedId(object));
        }
    }

    public static void installBridge() {
        KahluaTable environment = LuaManager.env;
        if (environment == null) return;
        ACTIVE.remove();
        environment.rawset("ConsoleFixesFloorPlacement", (JavaFunction)(frame, arguments) -> {
            Object command = frame.get(0);
            if ("begin".equals(command)) return frame.push(begin((IsoGridSquare)frame.get(1)));
            if ("close".equals(command)) {
                close((Scope)frame.get(1));
                return 0;
            }
            throw new IllegalArgumentException("Unknown floor placement command");
        });
    }

    public static Scope begin(IsoGridSquare square) {
        if (square == null) throw new IllegalArgumentException("Missing floor square");
        Scope scope = new Scope(square, ACTIVE.get());
        ACTIVE.set(scope);
        return scope;
    }

    public static boolean deferRegistration(GameEntity entity) {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.finishing || !(entity instanceof IsoObject)) return false;
        IsoObject object = (IsoObject)entity;
        if (!object.isFloor() || object.getSquare() == null || scope.oldIds.containsKey(object)) return false;
        if (scope.floor != null && scope.floor != object) return false;
        // A chained builder can redirect to an upper square. Bind before its first registration.
        if (scope.square != object.getSquare()) {
            scope.square = object.getSquare();
            scope.original.clear();
            scope.oldIds.clear();
            for (IsoObject existing : objects(scope.square)) {
                if (existing != object) {
                    scope.original.add(existing);
                    scope.oldIds.put(existing, cachedId(existing));
                }
            }
        }
        scope.floor = object;
        return true;
    }

    /** Runs before the native serializer, after native removal packets. */
    public static void beforeTransmit(IsoObject object) {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.floor != object || scope.finished) return;
        if (!scope.square.getObjects().contains(object)) throw new IllegalStateException("Floor left its build square");
        for (IsoObject existing : objects(scope.square)) {
            if (existing != object && existing.isFloor() && !isRug(existing)) {
                throw new IllegalStateException("Native floor cleanup did not finish");
            }
        }
        List<IsoObject> ordered = new ArrayList<>();
        ordered.add(object);
        for (IsoObject existing : scope.original) {
            if (scope.square.getObjects().contains(existing)) ordered.add(existing);
        }
        for (IsoObject existing : objects(scope.square)) {
            if (!ordered.contains(existing)) ordered.add(existing);
        }
        scope.finishing = true;
        try {
            remap(scope.square, ordered, object, scope.oldIds);
            scope.finished = true;
        } finally {
            scope.finishing = false;
        }
    }

    /** The native receiver already inserts at the transmitted index; only its ID cache can lag. */
    public static void afterRemoteLoad(IsoObject object, boolean addToObjects) {
        if (!GameClient.client || !addToObjects || object.getSquare() == null || !object.isFloor()
                || object.getObjectIndex() != 0) return;
        List<IsoObject> objects = objects(object.getSquare());
        for (IsoObject existing : objects) {
            if (existing != object && existing.isFloor() && !isRug(existing)) return;
        }
        remap(object.getSquare(), objects, null, new IdentityHashMap<>());
    }

    /** Always releases the scope, including exceptions before the floor's first packet. */
    public static void close(Scope scope) {
        if (ACTIVE.get() != scope) throw new IllegalStateException("Floor placement scope order changed");
        try {
            if (!scope.finished && scope.floor != null && scope.square.getObjects().contains(scope.floor)) {
                scope.floor.removeFromWorld();
                scope.floor.removeFromSquare();
                remap(scope.square, objects(scope.square), null, scope.oldIds);
            }
        } finally {
            if (scope.parent == null) ACTIVE.remove();
            else ACTIVE.set(scope.parent);
        }
    }

    private static boolean isRug(IsoObject object) {
        String texture = object.getTextureName();
        return texture != null && texture.contains("floors_rugs");
    }

    private static List<IsoObject> objects(IsoGridSquare square) {
        List<IsoObject> result = new ArrayList<>(square.getObjects().size());
        for (int index = 0; index < square.getObjects().size(); index++) result.add(square.getObjects().get(index));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void remap(IsoGridSquare square, List<IsoObject> ordered, IsoObject registerFloor,
                              IdentityHashMap<IsoObject, Long> originalIds) {
        try {
            LongMap<GameEntity> entities = (LongMap<GameEntity>)ENTITY_MAP.get(null);
            synchronized (entities) {
                IdentityHashMap<IsoObject, Boolean> mapped = new IdentityHashMap<>();
                Map<Long, IsoObject> removals = new LinkedHashMap<>();
                Map<Long, IsoObject> destinations = new LinkedHashMap<>();
                for (IsoObject object : ordered) {
                    long cached = cachedId(object);
                    if (cached != -1 && entities.get(cached) == object) removals.put(cached, object);
                    Long original = originalIds.get(object);
                    if (original != null && original != -1 && entities.get(original) == object) removals.put(original, object);
                }
                for (IsoObject object : removals.values()) mapped.put(object, Boolean.TRUE);
                for (int index = 0; index < ordered.size(); index++) {
                    IsoObject object = ordered.get(index);
                    if (object == registerFloor || REGISTERED.getBoolean(object) || mapped.containsKey(object)) {
                        long id = id(square, object.isFloor() ? 0 : index);
                        IsoObject duplicate = destinations.put(id, object);
                        if (duplicate != null && duplicate != object) throw new IllegalStateException("Multiple registered floors");
                        GameEntity occupant = entities.get(id);
                        if (occupant != null && !mapped.containsKey(occupant)) {
                            throw new IllegalStateException("Floor index destination belongs to another entity");
                        }
                    }
                }
                // Remove all owned keys before a permutation. No transient ID collision or recursive lazy lookup.
                for (Long key : removals.keySet()) entities.remove(key);
                square.getObjects().clear();
                square.getObjects().addAll(ordered);
                for (int index = 0; index < ordered.size(); index++) {
                    IsoObject object = ordered.get(index);
                    int entityIndex = object.isFloor() ? 0 : index;
                    NET_ID.setLong(object, id(square, entityIndex));
                    LAST_INDEX.setInt(object, entityIndex);
                }
                for (Map.Entry<Long, IsoObject> entry : destinations.entrySet()) {
                    if (entry.getValue() != registerFloor) entities.put(entry.getKey(), entry.getValue());
                }
                if (registerFloor != null) REGISTER.invoke(null, registerFloor);
            }
        } catch (InvocationTargetException failure) {
            throw new IllegalStateException("Native floor registration failed", failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Unsupported floor entity registry", failure);
        }
    }

    private static long id(IsoGridSquare square, int index) {
        return square.getX() + ((long)square.getY() << 16) + ((long)square.getZ() << 32) + ((long)index << 40);
    }

    private static long cachedId(IsoObject object) {
        try { return NET_ID.getLong(object); }
        catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }

    private static Method registrationMethod() {
        try {
            Method method = GameEntityManager.class.getDeclaredMethod("RegisterEntity", GameEntity.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
}
