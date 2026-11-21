package antikristianos.build42consolefixes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.properties.IsoObjectChange;
import zombie.debug.DebugOptions;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

/** Rejects missing-key requests before Vanilla spends starter battery charge. */
public final class VehicleStartAuthorization {
    private static final Method FAILED_WITH_ZOMBIES;
    static {
        try {
            FAILED_WITH_ZOMBIES = BaseVehicle.class.getDeclaredMethod("checkVehicleFailsToStartWithZombiesTargeting");
            FAILED_WITH_ZOMBIES.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
    private VehicleStartAuthorization() { }

    public static boolean reject(BaseVehicle vehicle, boolean haveKey) {
        // Keep Vanilla's movement, missing-engine and non-idle early returns.
        IsoGameCharacter driver = vehicle.getDriver();
        if (driver == null || driver instanceof IsoPlayer && ((IsoPlayer) driver).isBlockMovement()
                || vehicle.getPartById("Engine") == null
                || vehicle.getEngineState() != BaseVehicle.engineStateTypes.Idle) return false;
        if (haveKey || vehicle.isKeysInIgnition() || vehicle.isHotwired()
                || SandboxOptions.instance.vehicleEasyUse.getValue()
                || Core.debug && DebugOptions.instance.cheat.vehicle.startWithoutKey.getValue()) return false;

        if (GameServer.server) {
            driver.sendObjectChange(IsoObjectChange.VEHICLE_NO_KEY);
        } else {
            driver.SayDebug(" [img=media/ui/CarKey_none.png]");
            try {
                FAILED_WITH_ZOMBIES.invoke(vehicle);
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException(failure);
            } catch (InvocationTargetException failure) {
                Throwable cause = failure.getCause();
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw new IllegalStateException(cause);
            }
        }
        return true;
    }
}
