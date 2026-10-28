package antikristianos.build42consolefixes;

import java.util.List;

/** Keeps server-managed mods out of the client's local translation-mod prefix. */
public final class MultiplayerTranslationModOrder {
    private MultiplayerTranslationModOrder() {
    }

    public static void removeServerManagedEntries(
            List<String> translationMods,
            List<String> serverMods) {
        if (translationMods == null || translationMods.isEmpty()
                || serverMods == null || serverMods.isEmpty()) {
            return;
        }

        translationMods.removeIf(serverMods::contains);
    }
}
