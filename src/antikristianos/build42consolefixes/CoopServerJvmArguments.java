package antikristianos.build42consolefixes;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.zed_0xff.zombie_buddy.Agent;
import me.zed_0xff.zombie_buddy.Patch;

/** Adds the installed ZombieBuddy to the spawned Coop JVM on each supported platform. */
public final class CoopServerJvmArguments {
    private static final List<String> SHARED_OPTIONS = List.of(
            "policy", "allow_unsigned_mods", "frontend", "config_dir", "verbosity");

    private CoopServerJvmArguments() {}

    static String zombieBuddyAgent(Path dependency) throws IOException {
        if (zombieBuddyOwnsCoopLaunch(Agent.class.getClassLoader())) return null;
        return zombieBuddyAgent(System.getProperty("os.name", ""), dependency, Agent.arguments);
    }

    static boolean zombieBuddyOwnsCoopLaunch(ClassLoader loader) {
        try {
            // Match the capability proposed in upstream PR #63, without initializing it.
            Class<?> helper = Class.forName("me.zed_0xff.zombie_buddy.CoopServerJvmArguments", false, loader);
            var method = helper.getMethod("inherit", List.class);
            return Modifier.isStatic(method.getModifiers()) && method.getReturnType() == List.class;
        } catch (ReflectiveOperationException | LinkageError | SecurityException unavailable) {
            return false;
        }
    }

    static String zombieBuddyAgent(String os, Path dependency, Map<String, String> options)
            throws IOException {
        Path installed = dependency.toRealPath();
        if (!Files.isRegularFile(installed)) throw new IOException("ZombieBuddy JAR is not a file: " + installed);
        String agent = os.toLowerCase(Locale.ROOT).startsWith("windows")
                ? "-agentlib:zbNative"
                : "-javaagent:" + installed;
        List<String> inherited = new ArrayList<>();
        for (String key : SHARED_OPTIONS) {
            String value = options.get(key);
            if (value != null) {
                if (value.indexOf(',') >= 0) throw new IOException("Invalid ZombieBuddy option: " + key);
                inherited.add(key + "=" + value);
            }
        }
        if (!inherited.isEmpty()) agent += "=" + String.join(",", inherited);
        if (agent.indexOf('\n') >= 0 || agent.indexOf('\r') >= 0 || agent.indexOf('\0') >= 0) {
            throw new IOException("Invalid line break or NUL in ZombieBuddy launch argument");
        }
        return agent;
    }

    public static String includeZombieBuddyAgent(String garbageCollectorArgument) {
        if (zombieBuddyOwnsCoopLaunch(Agent.class.getClassLoader())) return garbageCollectorArgument;
        try {
            String agent = zombieBuddyAgent(CoopWorkshopLaunch.jarOf(Patch.class));
            if (agent == null) return garbageCollectorArgument;
            if (garbageCollectorArgument == null || garbageCollectorArgument.isBlank()) return agent;
            Path argumentFile = Files.createTempFile("build42-console-fixes-coop-", ".args");
            argumentFile.toFile().deleteOnExit();
            Files.write(argumentFile, List.of(
                    CoopWorkshopFiles.quoteArgument(garbageCollectorArgument),
                    CoopWorkshopFiles.quoteArgument(agent)), StandardCharsets.UTF_8);
            return "@" + argumentFile.toAbsolutePath();
        } catch (IOException exception) {
            System.err.println("[Build42ConsoleFixes] Could not prepare ZombieBuddy for the Coop Server JVM: "
                    + exception.getMessage());
            return garbageCollectorArgument;
        }
    }
}
