package antikristianos.build42consolefixes;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import zombie.core.znet.SteamUtils;
import zombie.network.*;

/** Only the client-spawned Coop JVM calls this instead of its separate Workshop installer. */
public final class CoopWorkshopHost {
    private CoopWorkshopHost() {}
    public static boolean enabled() {
        return GameServer.server && GameServer.coop && System.getProperty(CoopWorkshopFiles.PROPERTY) != null;
    }
    public static boolean install(List<Long> ids) {
        Path directory = Path.of(System.getProperty(CoopWorkshopFiles.PROPERTY));
        try {
            CoopWorkshopFiles.write(directory.resolve("request.properties"), CoopWorkshopFiles.request(ids));
            long deadline = System.nanoTime() + 1_800_000_000_000L;
            Path response = directory.resolve("response.properties");
            while (!Files.isRegularFile(response)) {
                ProcessHandle parent = ProcessHandle.current().parent().orElse(null);
                if (parent == null || !parent.isAlive()) throw new IOException("Coop client exited");
                if (System.nanoTime() >= deadline) throw new IOException("Client Workshop installation timed out");
                SteamUtils.runLoop();
                Thread.sleep(50);
            }
            apply(ids, CoopWorkshopFiles.read(response));
            System.out.println("[Build42ConsoleFixes] Coop uses client Steam Workshop: " + ids.size() + " items");
            return true;
        } catch (IOException | RuntimeException ex) {
            System.err.println("[Build42ConsoleFixes] Client Workshop unavailable: " + ex.getMessage());
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    public static void apply(List<Long> ids, Properties response) throws IOException {
        if (response.containsKey("error")) throw new IOException(response.getProperty("error"));
        if (!ids.equals(CoopWorkshopFiles.ids(response))) throw new IOException("Workshop item order/IDs differ");
        String[] paths = new String[ids.size()];
        long[] timestamps = new long[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            try {
                Path path = Path.of(response.getProperty(i + ".path"));
                if (!path.isAbsolute() || !Files.isDirectory(path) || !Files.isReadable(path)) {
                    throw new IOException("Client Workshop directory unavailable: " + path);
                }
                paths[i] = path.toRealPath().toString();
                timestamps[i] = Long.parseLong(response.getProperty(i + ".timestamp"));
                if (timestamps[i] <= 0) throw new IOException("Invalid Workshop installation timestamp");
            } catch (RuntimeException ex) { throw new IOException("Invalid client Workshop response", ex); }
        }
        GameServer.workshopInstallFolders = paths;
        GameServer.workshopTimeStamps = timestamps;
    }
}