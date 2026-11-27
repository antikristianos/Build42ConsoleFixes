package antikristianos.build42consolefixes;

import java.lang.reflect.Field;
import java.util.Map;
import zombie.network.GameClient;
import zombie.network.GameServer;

/** Clear persisted activation before Vanilla reactivates the scripts present on this server. */
public final class ScriptRegisterState {
    private ScriptRegisterState() { }

    public static void beforeParse(Object register) {
        if (!GameServer.server || GameClient.client) return;
        try {
            Map<?, ?> entries = (Map<?, ?>) Access.NAMED_MAP.get(register);
            for (Object info : entries.values()) Access.IS_LOADED.setBoolean(info, false);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("ConsoleFixes: cannot refresh script register activation", failure);
        }
    }

    private static final class Access {
        static final Field NAMED_MAP = field("zombie.world.ScriptsDictionary$ScriptRegister", "namedMap");
        static final Field IS_LOADED = field("zombie.world.DictionaryScriptInfo", "isLoaded");

        private static Field field(String className, String fieldName) {
            try {
                Field field = Class.forName(className).getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException failure) {
                throw new ExceptionInInitializerError(failure);
            }
        }
    }
}
