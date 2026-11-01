package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.network.ByteBufferReader;
import zombie.network.IConnection;
import zombie.network.PZNetKahluaTableImpl;

@Patch(className = "zombie.network.PZNetKahluaTableImpl", methodName = "load", warmUp = true, strictMatch = true)
public final class PatchHandcraftBenchLoad {
    private PatchHandcraftBenchLoad() { }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.This PZNetKahluaTableImpl table,
            @Patch.Argument(0) ByteBufferReader input, @Patch.Argument(1) IConnection connection,
            @Patch.Argument(2) byte valueType, @Patch.Argument(3) int depth,
            @Patch.Local("handcraftBench") Object bench) {
        if (!HandcraftBenchReference.handles(table, valueType, depth)) {
            return false;
        }
        bench = HandcraftBenchReference.read(table, input);
        return true;
    }

    @Patch.OnExit
    public static void exit(@Patch.Local("handcraftBench") Object bench,
            @Patch.Return(readOnly = false) Object returned) {
        // A non-bench return must remain untouched when the enter advice did not skip.
        if (bench != null) {
            returned = bench;
        }
    }
}
