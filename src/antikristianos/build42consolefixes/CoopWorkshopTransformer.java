package antikristianos.build42consolefixes;

import java.lang.instrument.Instrumentation;
import java.util.List;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import static net.bytebuddy.matcher.ElementMatchers.*;

/** Registered before GameServer.main and therefore before Workshop installation/mod discovery. */
public final class CoopWorkshopTransformer {
    private CoopWorkshopTransformer() {}
    public static void install(Instrumentation instrumentation) throws ClassNotFoundException {
        var transformed = new java.util.concurrent.atomic.AtomicBoolean();
        new AgentBuilder.Default().disableClassFormatChanges()
                .with(new AgentBuilder.Listener.Compound(
                        AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly(),
                        new AgentBuilder.Listener.Adapter() {
                            @Override public void onTransformation(net.bytebuddy.description.type.TypeDescription type,
                                    ClassLoader loader, net.bytebuddy.utility.JavaModule module, boolean loaded,
                                    net.bytebuddy.dynamic.DynamicType dynamicType) {
                                transformed.set(true);
                            }
                        }))
                .type(named("zombie.network.GameServerWorkshopItems"))
                .transform((builder, type, loader, module, domain) -> builder.visit(
                        Advice.to(InstallAdvice.class).on(named("Install").and(isStatic())
                                .and(returns(boolean.class)).and(takesArguments(1))
                                .and(takesArgument(0, isSubTypeOf(List.class))))))
                .installOn(instrumentation);
        Class<?> target = Class.forName("zombie.network.GameServerWorkshopItems", false, ClassLoader.getSystemClassLoader());
        long matches = java.util.Arrays.stream(target.getDeclaredMethods()).filter(method ->
                method.getName().equals("Install") && java.lang.reflect.Modifier.isStatic(method.getModifiers())
                && method.getReturnType() == boolean.class && method.getParameterCount() == 1
                && List.class.isAssignableFrom(method.getParameterTypes()[0])).count();
        if (matches != 1) throw new IllegalStateException("Unsupported Coop Workshop installer signature");
        if (!transformed.get()) throw new IllegalStateException("Early Coop Workshop hook was not installed");
    }
    public static final class InstallAdvice {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static Boolean enter(@Advice.Argument(0) List<Long> ids) {
            return CoopWorkshopHost.enabled() ? CoopWorkshopHost.install(ids) : null;
        }
        @Advice.OnMethodExit
        public static void exit(@Advice.Enter Boolean result, @Advice.Return(readOnly = false) boolean returned) {
            if (result != null) returned = result;
        }
    }
}