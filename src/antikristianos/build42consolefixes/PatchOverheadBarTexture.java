package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.Color;
import zombie.core.textures.Texture;
import zombie.ui.UIElement;

@Patch(className = "zombie.ui.UIElement", methodName = "DrawUVSliceTexture", warmUp = true, strictMatch = true)
public final class PatchOverheadBarTexture {
    private PatchOverheadBarTexture() { }
    @Patch.OnEnter
    public static void enter(@Patch.This UIElement element, @Patch.Argument(0) Texture texture,
            @Patch.Argument(value = 1, readOnly = false) double x,
            @Patch.Argument(value = 2, readOnly = false) double y,
            @Patch.Argument(value = 3, readOnly = false) double width,
            @Patch.Argument(value = 4, readOnly = false) double height,
            @Patch.Argument(5) Color color, @Patch.Argument(6) double u0,
            @Patch.Argument(7) double v0, @Patch.Argument(8) double u1, @Patch.Argument(9) double v1) {
        OverheadDisplayScale.Slice slice = OverheadDisplayScale.barSlice(element, texture, x, y, width, height);
        if (slice != null) {
            x = slice.x; y = slice.y; width = slice.width; height = slice.height;
        }
    }
}
