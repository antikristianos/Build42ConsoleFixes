package antikristianos.build42consolefixes;

import java.util.List;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.network.GameClient;

@Patch(
        className = "zombie.ZomboidFileSystem",
        methodName = "loadTranslationMods",
        warmUp = true,
        strictMatch = true)
public final class PatchMultiplayerTranslationModOrder {
    private PatchMultiplayerTranslationModOrder() {
    }

    @Patch.OnExit
    public static void exit(@Patch.Argument(0) List<String> translationMods) {
        if (GameClient.client && GameClient.instance != null) {
            MultiplayerTranslationModOrder.removeServerManagedEntries(
                    translationMods,
                    GameClient.instance.serverMods);
        }
    }
}
