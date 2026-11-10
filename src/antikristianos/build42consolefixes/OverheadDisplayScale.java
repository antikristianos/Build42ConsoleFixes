package antikristianos.build42consolefixes;

import java.lang.reflect.Field;
import java.util.WeakHashMap;
import zombie.characters.IsoGameCharacter;
import zombie.chat.ChatElement;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.network.GameServer;
import zombie.ui.TextDrawObject;
import zombie.ui.UIElement;

/** Session-only resolution policy; native text parsing, action state and drawing stay in Vanilla. */
public final class OverheadDisplayScale {
    private static volatile float scale = 1;
    private static final Field CHAT_OWNER = field(ChatElement.class, "owner");
    private static final Field CHAT_OUTER = field(chatLinesClass(), "this$0");
    private static final ThreadLocal<Scope> SCOPE = ThreadLocal.withInitial(Scope::new);
    private static final WeakHashMap<TextDrawObject, Metrics> TEXT = new WeakHashMap<>();
    private static final WeakHashMap<UIElement, Bar> BARS = new WeakHashMap<>();
    private static final WeakHashMap<Texture, ImageSize> IMAGES = new WeakHashMap<>();
    static int metricBuilds, barBuilds, sliceBuilds, imageBuilds, resolutionReads;

    private OverheadDisplayScale() { }

    public static float forHeight(int height) {
        return height >= 2160 ? 2 : height >= 1440 ? 1.5f : 1;
    }

    public static void startSession() {
        if (GameServer.server) return;
        resolutionReads++;
        startSession(Core.getInstance().getScreenHeight());
    }

    static void startSession(int height) {
        scale = forHeight(height);
        TEXT.clear();
        BARS.clear();
        IMAGES.clear();
        SCOPE.remove();
    }

    public static boolean beginChat(Object lines) {
        if (scale == 1 || GameServer.server) return false;
        try {
            if (!(CHAT_OWNER.get(CHAT_OUTER.get(lines)) instanceof IsoGameCharacter)) return false;
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
        }
        SCOPE.get().chatDepth++;
        return true;
    }

    public static void endChat() { SCOPE.get().chatDepth--; }

    public static boolean beginText(double x, double y) {
        Scope scope = SCOPE.get();
        if (scope.chatDepth == 0) return false;
        if (scope.rawDepth == scope.x.length) {
            scope.x = java.util.Arrays.copyOf(scope.x, scope.x.length * 2);
            scope.y = java.util.Arrays.copyOf(scope.y, scope.y.length * 2);
        }
        scope.x[scope.rawDepth] = x;
        scope.y[scope.rawDepth++] = y;
        return true;
    }

    public static void endText() { SCOPE.get().rawDepth--; }
    public static boolean drawingText() { return SCOPE.get().rawDepth > 0; }
    public static float textScale() { return scale; }
    public static double textX(double x) {
        Scope scope = SCOPE.get();
        double anchor = scope.x[scope.rawDepth - 1];
        return anchor + (x - anchor) * scale;
    }
    public static double textY(double y) {
        Scope scope = SCOPE.get();
        double anchor = scope.y[scope.rawDepth - 1];
        return anchor + (y - anchor) * scale;
    }

    public static ImageSize imageSize(Texture texture, int width, int height) {
        ImageSize size = IMAGES.computeIfAbsent(texture, ignored -> new ImageSize());
        if (size.nativeWidth != width || size.nativeHeight != height) {
            size.nativeWidth = width; size.nativeHeight = height;
            size.width = Math.round(width * scale); size.height = Math.round(height * scale);
            imageBuilds++;
        }
        return size;
    }

    public static int metric(TextDrawObject text, int nativeSize, boolean width) {
        if (SCOPE.get().chatDepth == 0) return nativeSize;
        Metrics metrics = TEXT.computeIfAbsent(text, ignored -> new Metrics());
        if (width) {
            if (metrics.width != nativeSize) {
                metrics.width = nativeSize;
                metrics.scaledWidth = (int)Math.ceil(nativeSize * scale);
                metricBuilds++;
            }
            return metrics.scaledWidth;
        }
        if (metrics.height != nativeSize) {
            metrics.height = nativeSize;
            metrics.scaledHeight = (int)Math.ceil(nativeSize * scale);
            metricBuilds++;
        }
        return metrics.scaledHeight;
    }

    /** Called after native positioning, which supplies fresh unscaled dimensions each time. */
    public static void positionBar(UIElement element, int playerIndex) {
        if (scale == 1 || GameServer.server || zombie.characters.IsoPlayer.players[playerIndex] == null) return;
        Bar bar = BARS.computeIfAbsent(element, ignored -> new Bar());
        float width = element.getWidth().floatValue(), height = element.getHeight().floatValue();
        if (bar.width != width || bar.height != height) {
            bar.width = width;
            bar.height = height;
            bar.scaledWidth = width * scale;
            bar.scaledHeight = height * scale;
            bar.shiftX = (bar.scaledWidth - width) / 2;
            bar.shiftY = bar.scaledHeight - height;
            barBuilds++;
        }
        element.setWidth(bar.scaledWidth);
        element.setHeight(bar.scaledHeight);
        element.setX(element.getX() - bar.shiftX);
        element.setY(element.getY() - bar.shiftY);
    }

    public static Slice barSlice(UIElement element, Texture texture, double x, double y, double w, double h) {
        if (scale == 1 || GameServer.server || !(element instanceof zombie.ui.ActionProgressBar) || texture == null) return null;
        Bar bar = BARS.computeIfAbsent(element, ignored -> new Bar());
        Slice slice = bar.first;
        if (slice == null) bar.first = slice = new Slice();
        else if (slice.texture != texture) {
            slice = bar.second;
            if (slice == null) bar.second = slice = new Slice();
        }
        if (slice.texture != texture || slice.nativeX != x || slice.nativeY != y || slice.nativeW != w
                || slice.nativeH != h || slice.offsetX != texture.offsetX || slice.offsetY != texture.offsetY) {
            slice.texture = texture;
            slice.nativeX = x; slice.nativeY = y; slice.nativeW = w; slice.nativeH = h;
            slice.offsetX = texture.offsetX; slice.offsetY = texture.offsetY;
            // UIElement adds texture offsets after the supplied coordinates.
            slice.x = (x + texture.offsetX) * scale - texture.offsetX;
            slice.y = (y + texture.offsetY) * scale - texture.offsetY;
            slice.width = w * scale; slice.height = h * scale;
            sliceBuilds++;
        }
        return slice;
    }

    private static Class<?> chatLinesClass() {
        try { return Class.forName("zombie.chat.ChatElement$PlayerLines"); }
        catch (ClassNotFoundException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private static Field field(Class<?> type, String name) {
        try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private static final class Scope {
        int chatDepth, rawDepth;
        double[] x = new double[4], y = new double[4];
    }
    private static final class Metrics {
        int width = -1, height = -1, scaledWidth, scaledHeight;
    }
    private static final class Bar {
        float width = -1, height = -1, scaledWidth, scaledHeight, shiftX, shiftY;
        Slice first, second;
    }
    public static final class Slice {
        Texture texture;
        double nativeX, nativeY, nativeW, nativeH;
        float offsetX, offsetY;
        public double x, y, width, height;
    }
    public static final class ImageSize {
        int nativeWidth = -1, nativeHeight = -1;
        public int width, height;
    }
}
