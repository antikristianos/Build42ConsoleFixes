package antikristianos.build42consolefixes;

import java.util.concurrent.locks.ReentrantLock;

/** Shares one reentrant gate between directory discovery, prefix publication and invalidation. */
public final class ModFolderScanGuard {
    private static final ReentrantLock LOCK = new ReentrantLock();
    // 42.21 publishes a complete list under the native monitor. Combining that
    // monitor with this older external gate would reverse lock order on discovery.
    private static final boolean NATIVE_SNAPSHOT = hasNativeSnapshot();

    private static boolean hasNativeSnapshot() {
        try {
            return java.lang.reflect.Modifier.isSynchronized(
                    zombie.ZomboidFileSystem.class.getDeclaredMethod(
                            "getAllModFolders", java.util.List.class).getModifiers());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unsupported mod-folder discovery API", e);
        }
    }

    private ModFolderScanGuard() {
    }

    public static void enter() {
        if (!NATIVE_SNAPSHOT) LOCK.lock();
    }

    public static void exit() {
        if (!NATIVE_SNAPSHOT) LOCK.unlock();
    }
}
