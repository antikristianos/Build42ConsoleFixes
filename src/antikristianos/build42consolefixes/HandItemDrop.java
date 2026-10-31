package antikristianos.build42consolefixes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugType;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.PlayerDropHeldItemsPacket;
import zombie.util.flags.EnumByteFlags;

/** Repairs the shared drop entrypoint without changing any packet format. */
public final class HandItemDrop {
    /** Optional NPC integration. The owner supplies identity, authority and transport. */
    public interface NpcSynchronization {
        boolean supports(IsoGameCharacter character);
        boolean prepareDrop(IsoGameCharacter character, IsoGridSquare square);
        void itemDropped(IsoGameCharacter character, IsoGridSquare square,
                InventoryItem item, EnumByteFlags<PlayerDropHeldItemsPacket.Flag> flags);
        default void equipmentChanged(IsoGameCharacter character) { }
    }

    private static final Method DROP_SQUARE;
    private static final Field HANDS_DIRTY;
    private static final CopyOnWriteArrayList<NpcSynchronization> NPC_SYNCHRONIZATIONS = new CopyOnWriteArrayList<>();
    private static final Set<Class<?>> REPORTED_UNBOUND = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<DropScope> DROP_SCOPE = new ThreadLocal<>();

    static {
        try {
            DROP_SQUARE = IsoGameCharacter.class.getDeclaredMethod("getDropOnSquare");
            DROP_SQUARE.setAccessible(true);
            HANDS_DIRTY = IsoGameCharacter.class.getDeclaredField("handItemShouldSendToClients");
            HANDS_DIRTY.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private HandItemDrop() { }

    public static AutoCloseable registerNpcSynchronization(NpcSynchronization synchronization) {
        Objects.requireNonNull(synchronization, "synchronization");
        if (!NPC_SYNCHRONIZATIONS.addIfAbsent(synchronization)) {
            throw new IllegalArgumentException("NPC synchronization is already registered");
        }
        AtomicBoolean registered = new AtomicBoolean(true);
        return () -> {
            if (registered.compareAndSet(true, false)) NPC_SYNCHRONIZATIONS.remove(synchronization);
        };
    }

    public static boolean perform(IsoGameCharacter character, Predicate<IsoGridSquare> performDrop) {
        IsoGridSquare square = dropSquare(character);
        if (square == null) return false;
        if (!GameClient.client) return performDrop.test(square);

        if (hasPlayerAddress(character)) {
            if (character.isLocal()) INetworkPacket.send(PacketTypes.PacketType.Equip, character);
            return performDrop.test(square);
        }
        // Remote real players retain their native presentation path, without sending.
        if (character instanceof IsoPlayer && ((IsoPlayer) character).remote) {
            return performDrop.test(square);
        }
        if (character.getPrimaryHandItem() == null && character.getSecondaryHandItem() == null) {
            return performDrop.test(square);
        }
        // A nonplayer client cannot commit a world item without its owner's transport.
        if (!character.isLocal()) return false;
        NpcSynchronization synchronization = findSynchronization(character);
        if (synchronization == null) {
            if (REPORTED_UNBOUND.add(character.getClass())) {
                DebugType.General.warn("[Build42ConsoleFixes] NPC hand drop deferred for "
                        + character.getClass().getName() + ": no synchronization binding; held items retained.");
            }
            return false;
        }
        DropScope previous = DROP_SCOPE.get();
        if (previous != null && previous.character == character) {
            throw new IllegalStateException("Recursive NPC hand-drop preparation");
        }
        DROP_SCOPE.set(new DropScope(character, synchronization));
        try {
            if (!synchronization.prepareDrop(character, square)) return false;
            return performDrop.test(square);
        } finally {
            if (previous == null) DROP_SCOPE.remove();
            else DROP_SCOPE.set(previous);
        }
    }

    /** Handles only a prepared local NPC client drop; all other native bodies run. */
    public static boolean finishNpcDrop(IsoGameCharacter character, IsoGridSquare square,
            InventoryItem item, EnumByteFlags<PlayerDropHeldItemsPacket.Flag> flags) {
        if (!GameClient.client || hasPlayerAddress(character) || !character.isLocal()) return false;
        DropScope scope = DROP_SCOPE.get();
        if (scope == null || scope.character != character) {
            throw new IllegalStateException("NPC hand drop has no prepared synchronization scope");
        }
        LuaEventManager.triggerEvent("onItemFall", item);
        character.playDropItemSound(item);
        scope.synchronization.itemDropped(character, square, item, flags);
        return true;
    }

    /** Native dirty-hand replication is player-only; NPC owners keep their events. */
    public static void preparePreupdate(IsoGameCharacter character) {
        if (!GameServer.server || character instanceof IsoPlayer || !handsDirty(character)) return;
        notifyNpcEquipment(character);
        clearHandsDirty(character);
    }

    public static boolean updateNpcHands(IsoGameCharacter character) {
        if (!GameServer.server || character instanceof IsoPlayer) return false;
        notifyNpcEquipment(character);
        clearHandsDirty(character);
        return true;
    }

    private static boolean hasPlayerAddress(IsoGameCharacter character) {
        if (!(character instanceof IsoPlayer)) return false;
        IsoPlayer player = (IsoPlayer) character;
        int index = player.getIndex();
        return player.isLocalPlayer() && index >= 0 && index < IsoPlayer.players.length
                && IsoPlayer.players[index] == player;
    }

    private static NpcSynchronization findSynchronization(IsoGameCharacter character) {
        for (NpcSynchronization synchronization : NPC_SYNCHRONIZATIONS) {
            if (synchronization.supports(character)) return synchronization;
        }
        return null;
    }

    private static void notifyNpcEquipment(IsoGameCharacter character) {
        NpcSynchronization synchronization = findSynchronization(character);
        if (synchronization != null) synchronization.equipmentChanged(character);
    }

    private static IsoGridSquare dropSquare(IsoGameCharacter character) {
        try {
            return (IsoGridSquare) DROP_SQUARE.invoke(character);
        } catch (InvocationTargetException failure) {
            return throwUnchecked(failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Native drop-square lookup unavailable", failure);
        }
    }

    private static boolean handsDirty(IsoGameCharacter character) {
        try { return HANDS_DIRTY.getBoolean(character); }
        catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
    }

    private static void clearHandsDirty(IsoGameCharacter character) {
        try { HANDS_DIRTY.setBoolean(character, false); }
        catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }

    private static final class DropScope {
        final IsoGameCharacter character;
        final NpcSynchronization synchronization;
        DropScope(IsoGameCharacter character, NpcSynchronization synchronization) {
            this.character = character;
            this.synchronization = synchronization;
        }
    }
}
