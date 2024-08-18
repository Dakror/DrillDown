package de.dakror.modding.stub;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class StubAgent {
    private static final String AGENT_CLASS = System.getProperty("de.dakror.modding.agent.class", "de.dakror.modding.agent.ModAgent");
    private static final String AGENT_URL = System.getProperty("de.dakror.modding.agent.url"); // effective default of "ModLoader.jar", see findAgents()
    private static final boolean IS_DEV = AGENT_URL != null;
    private static final ClassLoader platformLoader = getPlatformClassLoader();

    static final Map<String, Agent> agents = new HashMap<>();
    static {
        // this must come BELOW the configurations above!
        findAgents();
    }

    public static void premain(String agentArgs, Instrumentation inst) throws Throwable {
        for (Agent agent: agents.values()) {
            agent.premain(agentArgs, inst);
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws Throwable {
        for (Agent agent: agents.values()) {
            agent.agentmain(agentArgs, inst);
        }
    }

    private static void findAgents() {
        final URL stubLocation = StubAgent.class.getProtectionDomain().getCodeSource().getLocation();
        
        try {
            if (AGENT_URL != null) {
                // Try to find a loader in an adjacent file specified manually
                tryLoading(stubLocation.toURI().resolve(AGENT_URL).toURL(), AGENT_CLASS);
            }
    
            // Try to find a loader just using the current classpath
            tryLoading(null, AGENT_CLASS);

            if (AGENT_URL == null) {
                // if no file specified manually, try to find ModLoader.jar
                tryLoading(stubLocation.toURI().resolve("ModLoader.jar").toURL(), AGENT_CLASS);
            }
    
            // Try to find a loader from the location of the stub, if that's different
            tryLoading(stubLocation, AGENT_CLASS);
        } catch (Throwable e) {
            System.err.print("While finding modloader agents: ");
            e.printStackTrace();
        }
    }

    private static ClassLoader getPlatformClassLoader() {
        try {
            return (ClassLoader) ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null);
        } catch (ReflectiveOperationException e) {
            // 1.8 doesn't have ClassLoader.getPlatformClassLoader, so just assume it's our parent classloader bc it doesn't matter
            return StubAgent.class.getClassLoader().getParent();
        }
    }

    private static void tryLoading(URL url, String agentName) {
        ClassLoader loader = url == null ? StubAgent.class.getClassLoader() : new StubLoader(new URL[] {url}, platformLoader);

        try {
            Agent agent = new Agent(loader.loadClass(agentName));
            agents.putIfAbsent(agent.getName(), agent); // prioritize the first agent loaded of that name
        } catch (ClassNotFoundException cnfe) {
            // This is not surprising, don't be noisy
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException)e).getTargetException();
            }
            System.err.print("While loading agent "+agentName+" from "+url+": ");
            e.printStackTrace();
        }
    }

    private static class Agent {
        public final Class<?> agentClass;

        private Agent(Class<?> agentClass) {
            this.agentClass = agentClass;
        }
        public void premain(Object... args) throws Throwable {
            safeCall("premain", args);
        }
        public void agentmain(Object... args) throws Throwable {
            safeCall("agentmain", args);
        }

        // just a name for this agent to make sure we don't load two of the same agent
        public String getName() throws Throwable {
            try {
                return (String) callMethod("getName");
            } catch (NoSuchMethodException e) {
                return agentClass.getName();
            }
        }

        // call a method, don't bail if we're not in a dev environment
        protected void safeCall(String methodName, Object... args) throws Throwable {
            try {
                callMethod(methodName, args);
            } catch (Throwable e) {
                if (IS_DEV) throw e;
                // otherwise print the stack trace and continue
                e.printStackTrace();
            }
        }

        // try calling a method, throw any exceptions
        protected Object callMethod(String methodName, Object... args) throws Throwable {
            for (Method method: agentClass.getMethods()) {
                if (!method.getName().equals(methodName) || !Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                try {
                    return method.invoke(null, args);
                } catch (IllegalArgumentException|IllegalAccessException e) {
                    // signature doesn't match, try again with the next method
                } catch (InvocationTargetException ite) {
                    throw ite.getTargetException();
                }
            }
            throw new NoSuchMethodException(methodName);
        }
    }

    private static class StubLoader extends URLClassLoader {
        public StubLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
    }
}
