package antikristianos.build42consolefixes;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.znet.SteamUtils;

public final class CoopWorkshopLaunch {
    private CoopWorkshopLaunch() {}
    public static String arguments(String garbageCollector) {
        if (!SteamUtils.isSteamModeEnabled()) {
            return CoopServerJvmArguments.includeZombieBuddyAgent(garbageCollector);
        }
        try {
            Path ownJar = jarOf(CoopWorkshopLaunch.class);
            Path dependency = jarOf(Patch.class);
            Path session = CoopWorkshopClient.prepare();
            Properties launch = new Properties();
            launch.setProperty("session", session.toString());
            launch.setProperty("zombieBuddy", dependency.toString());
            Path config = session.resolve("launch.properties");
            CoopWorkshopFiles.write(config, launch);
            return writeArguments(session.resolve("jvm.args"), garbageCollector, ownJar, config, CoopServerJvmArguments.zombieBuddyAgent(dependency));
        } catch (IOException | RuntimeException ex) {
            // Do not silently fall back to a second server Workshop installation.
            throw new IllegalStateException("Cannot prepare shared client Workshop for Coop", ex);
        }
    }
    static String writeArguments(Path target, String gc, Path jar, Path config, String zombieBuddyAgent) throws IOException {
        List<String> arguments = new ArrayList<>();
        if (gc != null && !gc.isBlank()) arguments.add(CoopWorkshopFiles.quoteArgument(gc));
        if (zombieBuddyAgent != null) arguments.add(CoopWorkshopFiles.quoteArgument(zombieBuddyAgent));
        arguments.add(CoopWorkshopFiles.quoteArgument("-javaagent:" + jar + "=" + config));
        Files.write(target, arguments, StandardCharsets.UTF_8);
        return "@" + target.toAbsolutePath();
    }
    static Path jarOf(Class<?> type) throws IOException {
        try {
            var source = type.getProtectionDomain().getCodeSource();
            if (source != null) {
                Path location = Path.of(source.getLocation().toURI());
                if (Files.isRegularFile(location) && location.toString().endsWith(".jar")) return location.toRealPath();
            }
            URL resource = type.getResource("/" + type.getName().replace('.', '/') + ".class");
            if (resource != null && resource.openConnection() instanceof JarURLConnection connection) {
                return Path.of(connection.getJarFileURL().toURI()).toRealPath();
            }
        } catch (Exception ex) { throw new IOException("Cannot locate loaded JAR for " + type.getName(), ex); }
        throw new IOException("No loaded JAR location for " + type.getName());
    }
}
