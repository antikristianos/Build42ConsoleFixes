package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.NetTimedAction;
import zombie.core.network.ByteBufferReader;
import zombie.network.IConnection;

@Patch(className = "zombie.core.NetTimedAction", methodName = "parse", warmUp = true, strictMatch = true)
public final class PatchHandcraftRequestParse {
    private PatchHandcraftRequestParse() { }

    @Patch.OnEnter
    public static void enter(@Patch.This NetTimedAction action,
            @Patch.Argument(0) ByteBufferReader input, @Patch.Argument(1) IConnection connection,
            @Patch.Local("handcraftBenchScope") HandcraftBenchReference.Scope scope) {
        scope = HandcraftBenchReference.enter(action);
    }

    @Patch.OnExit(onThrowable = Throwable.class)
    public static void exit(@Patch.Local("handcraftBenchScope") HandcraftBenchReference.Scope scope) {
        HandcraftBenchReference.exit(scope);
    }
}
