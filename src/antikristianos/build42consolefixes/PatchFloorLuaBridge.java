package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.Lua.LuaManager", methodName = "init", warmUp = true, strictMatch = true)
public final class PatchFloorLuaBridge {
    private PatchFloorLuaBridge() { }
    @Patch.OnExit
    public static void exit() { FloorPlacement.installBridge(); }
}
