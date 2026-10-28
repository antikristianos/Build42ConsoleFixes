package antikristianos.build42consolefixes;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Per-launch local IPC; no game files, links or shared Workshop metadata are written. */
public final class CoopWorkshopFiles {
    public static final String PROPERTY = "build42consolefixes.coopWorkshop";
    private CoopWorkshopFiles() {}
    public static Properties read(Path file) throws IOException {
        Properties result = new Properties();
        try (InputStream in = Files.newInputStream(file)) { result.load(in); }
        return result;
    }
    public static void write(Path file, Properties value) throws IOException {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(temp)) { value.store(out, "Build42ConsoleFixes local Coop"); }
        Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    public static List<Long> ids(Properties data) throws IOException {
        try {
            int count = Integer.parseInt(data.getProperty("count"));
            if (count < 0 || count > 10000) throw new IllegalArgumentException("item count");
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                long id = Long.parseUnsignedLong(data.getProperty(i + ".id"));
                if (id == 0) throw new IllegalArgumentException("zero item ID");
                ids.add(id);
            }
            return ids;
        } catch (RuntimeException ex) { throw new IOException("Invalid Coop Workshop request", ex); }
    }
    public static Properties request(List<Long> ids) {
        Properties result = new Properties();
        result.setProperty("count", Integer.toString(ids.size()));
        for (int i = 0; i < ids.size(); i++) result.setProperty(i + ".id", Long.toUnsignedString(ids.get(i)));
        return result;
    }
    public static String quoteArgument(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}