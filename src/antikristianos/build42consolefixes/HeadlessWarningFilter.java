package antikristianos.build42consolefixes;

/** Selects diagnostics that are provably meaningless in a headless server process. */
public final class HeadlessWarningFilter {
    private static final String MISSING_XUI_ICON_PREFIX = "Could not find icon:";

    private HeadlessWarningFilter() {
    }

    public static boolean shouldSuppress(boolean dedicatedServer, Object message) {
        return dedicatedServer
                && message != null
                && message.toString().startsWith(MISSING_XUI_ICON_PREFIX);
    }
}
