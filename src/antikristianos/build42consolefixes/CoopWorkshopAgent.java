package antikristianos.build42consolefixes;

import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.util.Properties;
import java.util.jar.JarFile;

/** The mod JAR also serves as an early, per-Coop-launch Java agent. */
public final class CoopWorkshopAgent {
    private CoopWorkshopAgent() {}
    public static void premain(String configFile, Instrumentation instrumentation) throws Exception {
        Properties config = CoopWorkshopFiles.read(Path.of(configFile));
        Path directory = Path.of(config.getProperty("session")).toRealPath();
        Path dependency = Path.of(config.getProperty("zombieBuddy")).toRealPath();
        // Reuse the already installed dependency; do not ship or modify a foreign JAR.
        instrumentation.appendToSystemClassLoaderSearch(new JarFile(dependency.toFile()));
        System.setProperty(CoopWorkshopFiles.PROPERTY, directory.toString());
        CoopWorkshopTransformer.install(instrumentation);
    }
}