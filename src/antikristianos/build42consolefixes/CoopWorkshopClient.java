package antikristianos.build42consolefixes;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import zombie.core.znet.*;
import zombie.core.znet.SteamWorkshopItem.ItemState;
import zombie.network.CoopMaster;

/** Runs from the client's normal Coop update, using the client's actual Steam context. */
public final class CoopWorkshopClient implements ISteamWorkshopCallback {
    private static Path session;
    private static CoopWorkshopClient active;
    private static long nextPoll;
    private final Path directory;
    private final List<Long> ids;
    private final SteamWorkshop steam = SteamWorkshop.instance;
    private final Map<Long, Long> updates = new HashMap<>();
    private final Set<Long> requested = new HashSet<>();
    private final Set<Long> completed = new HashSet<>();
    private long query;
    private boolean queried;
    private boolean done;
    private String error;

    private CoopWorkshopClient(Path directory, List<Long> ids) {
        this.directory = directory;
        this.ids = ids;
    }

    public static synchronized Path prepare() throws IOException {
        close();
        session = Files.createTempDirectory("build42-coop-workshop-");
        session.toFile().deleteOnExit();
        // Register in parent-first order: deleteOnExit removes children first.
        for (String name : List.of("request.properties", "request.properties.tmp",
                "response.properties", "response.properties.tmp", "launch.properties", "jvm.args")) {
            session.resolve(name).toFile().deleteOnExit();
        }
        return session;
    }

    public static synchronized void update() {
        update(session != null && CoopMaster.instance.isRunning());
    }

    static synchronized void update(boolean running) {
        if (session == null) return;
        if (!running) { close(); return; }
        if (System.nanoTime() < nextPoll) return;
        nextPoll = System.nanoTime() + 250_000_000L;
        try {
            if (active == null && Files.isRegularFile(session.resolve("request.properties"))) {
                active = new CoopWorkshopClient(session, CoopWorkshopFiles.ids(
                        CoopWorkshopFiles.read(session.resolve("request.properties"))));
                active.startQuery();
            }
            if (active != null) active.poll();
        } catch (Exception ex) {
            try {
                Properties failure = new Properties();
                failure.setProperty("error", ex.toString());
                CoopWorkshopFiles.write(session.resolve("response.properties"), failure);
            } catch (IOException writeFailure) {
                System.err.println("[Build42ConsoleFixes] Coop Workshop reply failed: " + writeFailure);
            }
            close();
        }
    }

    private static void close() {
        if (active != null) active.release();
        active = null;
        session = null;
    }

    private void startQuery() {
        if (ids.isEmpty()) { queried = true; return; }
        query = steam.CreateQueryUGCDetailsRequest(ids.stream().mapToLong(Long::longValue).toArray(), this);
        if (query == 0) error = "Client Steam Workshop query could not be started";
    }

    private void poll() throws IOException {
        if (done) return;
        if (error != null) {
            Properties failure = new Properties();
            failure.setProperty("error", error);
            CoopWorkshopFiles.write(directory.resolve("response.properties"), failure);
            done = true;
            release();
            return;
        }
        if (!queried) return;
        Properties result = CoopWorkshopFiles.request(ids);
        for (int i = 0; i < ids.size(); i++) {
            long id = ids.get(i);
            long state = steam.GetItemState(id);
            boolean busy = ItemState.Downloading.and(state) || ItemState.DownloadPending.and(state);
            boolean installed = ItemState.Installed.and(state);
            long timestamp = installed ? steam.GetItemInstallTimeStamp(id) : 0;
            boolean stale = ItemState.NeedsUpdate.and(state)
                    || (installed && updates.getOrDefault(id, 0L) != 0 && updates.get(id) != timestamp);
            if (busy) return;
            if (!installed || stale) {
                if (completed.contains(id)) {
                    error = "Client Steam finished download but the installed item is not current: " + Long.toUnsignedString(id);
                    return;
                }
                if (requested.add(id) && !steam.DownloadItem(id, true, this)) {
                    error = "Client Steam download could not be started for " + Long.toUnsignedString(id);
                }
                return;
            }
            String folder = steam.GetItemInstallFolder(id);
            if (folder == null || !Files.isDirectory(Path.of(folder))) {
                error = "Client Steam reports no installed folder for " + Long.toUnsignedString(id);
                return;
            }
            result.setProperty(i + ".path", Path.of(folder).toRealPath().toString());
            result.setProperty(i + ".timestamp", Long.toString(timestamp));
        }
        CoopWorkshopFiles.write(directory.resolve("response.properties"), result);
        done = true;
        release();
    }

    private void release() {
        if (query != 0) { steam.ReleaseQueryUGCRequest(query); query = 0; }
        steam.RemoveCallback(this);
    }
    @Override public void onItemQueryCompleted(long handle, int count) {
        if (handle != query) return;
        for (int i = 0; i < count; i++) {
            SteamUGCDetails item = steam.GetQueryUGCResult(handle, i);
            if (item != null) updates.put(item.getID(), item.getTimeUpdated());
        }
        if (!updates.keySet().containsAll(ids)) error = "Client Steam returned incomplete Workshop item details";
        queried = true;
        // Keep the callback registered for DownloadItem; release only the completed query.
        steam.ReleaseQueryUGCRequest(query);
        query = 0;
    }
    @Override public void onItemQueryNotCompleted(long handle, int code) {
        if (handle == query) error = "Client Steam Workshop query failed: " + code;
    }
    @Override public void onItemNotDownloaded(long id, int code) {
        if (requested.contains(id)) error = "Client Steam download failed for " + Long.toUnsignedString(id) + ": " + code;
    }
    @Override public void onItemDownloaded(long id) { if (requested.contains(id)) completed.add(id); }
    @Override public void onItemCreated(long id, boolean agreement) {}
    @Override public void onItemNotCreated(int code) {}
    @Override public void onItemUpdated(boolean agreement) {}
    @Override public void onItemNotUpdated(int code) {}
    @Override public void onItemSubscribed(long id) {}
    @Override public void onItemNotSubscribed(long id, int code) {}
}