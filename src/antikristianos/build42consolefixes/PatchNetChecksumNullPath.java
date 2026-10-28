package antikristianos.build42consolefixes;

import me.zed_0xff.zombie_buddy.Patch;

@Patch(
        className = "zombie.network.NetChecksum$Checksummer",
        methodName = "addFile",
        warmUp = true,
        strictMatch = true)
public final class PatchNetChecksumNullPath {
    private PatchNetChecksumNullPath() {
    }

    @Patch.OnEnter(skipOn = true)
    public static boolean enter(
            @Patch.Argument(0) String relativePath,
            @Patch.Argument(1) String absolutePath) {
        return absolutePath == null || absolutePath.isBlank();
    }
}
