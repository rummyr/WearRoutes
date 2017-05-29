package uk.me.ponies.wearroutes.common.logging;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Moronic isDebugEnabled class.
 */
public class DebugEnabled {
    private static boolean defaultEnablement = true;
    private static final Map<String, Boolean> enablements = new ConcurrentHashMap<>();
    private static final Set<String> notSet = Collections.synchronizedSet(new HashSet<String>());
    /** Hidden to prevent static instantiation. */
    private DebugEnabled() {}

    public static boolean tagEnabled(String tag) {
        Boolean b = enablements.get(tag);
        if (b == null && !notSet.contains(tag)) {
            notSet.add(tag);
        }
        if (b == null) {
            return defaultEnablement;
        } else {
            return b.booleanValue();
        }
    }

    public static void enableTag(String tag) {
        enablements.put(tag, Boolean.TRUE);
    }
    public static void disableTag(String tag) {
        enablements.put(tag, Boolean.FALSE);
    }

    public static void setDefaultEnablement(boolean b) {
        defaultEnablement = b;
    }

}
