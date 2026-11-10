package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.fonts.AngelCodeFont;

@Patch(className = "zombie.core.fonts.AngelCodeFont", methodName = "drawString", warmUp = true, strictMatch = true)
public final class PatchOverheadFont {
    private PatchOverheadFont() { }
    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.This AngelCodeFont font,
            @Patch.Argument(0) float x, @Patch.Argument(1) float y, @Patch.Argument(2) String text,
            @Patch.Argument(3) float r, @Patch.Argument(4) float g,
            @Patch.Argument(5) float b, @Patch.Argument(6) float a) {
        if (!OverheadDisplayScale.drawingText()) return false;
        font.drawString((float)OverheadDisplayScale.textX(x), (float)OverheadDisplayScale.textY(y),
                OverheadDisplayScale.textScale(), text, r, g, b, a);
        return true;
    }
}
