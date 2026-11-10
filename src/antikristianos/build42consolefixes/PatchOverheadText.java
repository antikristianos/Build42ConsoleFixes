package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.ui.TextDrawHorizontal;

@Patch(className = "zombie.ui.TextDrawObject", methodName = "DrawRaw", warmUp = true, strictMatch = true)
public final class PatchOverheadText {
    private PatchOverheadText() { }
    @Patch.OnEnter
    public static void enter(@Patch.Argument(0) TextDrawHorizontal horizontal,
            @Patch.Argument(1) double x, @Patch.Argument(2) double y,
            @Patch.Argument(3) float r, @Patch.Argument(4) float g,
            @Patch.Argument(5) float b, @Patch.Argument(6) float a,
            @Patch.Argument(7) boolean outlines, @Patch.Local("overheadText") boolean active) {
        active = OverheadDisplayScale.beginText(x, y);
    }
    @Patch.OnExit(onThrowable = Throwable.class)
    public static void exit(@Patch.Local("overheadText") boolean active) {
        if (active) OverheadDisplayScale.endText();
    }
}
