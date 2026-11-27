package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.world.ScriptsDictionary$ScriptRegister", methodName = "parseLoadList",
        warmUp = true, strictMatch = true)
public final class PatchScriptRegisterState {
    private PatchScriptRegisterState() { }

    @Patch.OnEnter
    public static void enter(@Patch.This Object register) {
        ScriptRegisterState.beforeParse(register);
    }
}
