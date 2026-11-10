package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(className = "zombie.chat.ChatElement$PlayerLines", methodName = "render", warmUp = true, strictMatch = true)
public final class PatchOverheadChat {
    private PatchOverheadChat() { }
    @Patch.OnEnter
    public static void enter(@Patch.This Object lines, @Patch.Local("overheadChat") boolean active) {
        active = OverheadDisplayScale.beginChat(lines);
    }
    @Patch.OnExit(onThrowable = Throwable.class)
    public static void exit(@Patch.Local("overheadChat") boolean active) {
        if (active) OverheadDisplayScale.endChat();
    }
}
