package antikristianos.build42consolefixes;

import java.util.function.Consumer;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;

@Patch(className = "zombie.core.SpriteRenderer", methodName = "renderi", warmUp = true, strictMatch = true)
public final class PatchOverheadImage {
    private PatchOverheadImage() { }
    @Patch.OnEnter
    public static void enter(@Patch.Argument(0) Texture texture,
            @Patch.Argument(value = 1, readOnly = false) int x,
            @Patch.Argument(value = 2, readOnly = false) int y,
            @Patch.Argument(value = 3, readOnly = false) int width,
            @Patch.Argument(value = 4, readOnly = false) int height,
            @Patch.Argument(5) float r, @Patch.Argument(6) float g,
            @Patch.Argument(7) float b, @Patch.Argument(8) float a,
            @Patch.Argument(9) Consumer<TextureDraw> callback) {
        if (OverheadDisplayScale.drawingText()) {
            x = (int)Math.round(OverheadDisplayScale.textX(x));
            y = (int)Math.round(OverheadDisplayScale.textY(y));
            OverheadDisplayScale.ImageSize size = OverheadDisplayScale.imageSize(texture, width, height);
            width = size.width;
            height = size.height;
        }
    }
}
