package uk.me.ponies.wearroutes.utils;

import android.util.Log;

import java.util.WeakHashMap;

import uk.me.ponies.wearroutes.common.Defeat;

/**
 * Test class to detect if more than once instance of something is running.
 * Ideally would be a mix-in class/annotation, but whatever
 */
public class SingleInstanceChecker {
    private static WeakHashMap<Class, Integer> classCounts = new WeakHashMap<>();
    private static WeakHashMap<Object, Integer> instanceCounts = new WeakHashMap<>();

    public SingleInstanceChecker(Object whichInstance) {
        Integer ONE = 1;
        if (!classCounts.containsKey(whichInstance.getClass())) {
            // never before seen instance
            classCounts.put(whichInstance.getClass(), ONE);
            instanceCounts.put(whichInstance, ONE);
        }
        else {
            // isNullAndLog that there is only ONE of this specific Object
            if (!ONE.equals(instanceCounts.get(whichInstance))) {
                Log.e(whichInstance.getClass().getSimpleName(), " Multiple Instances of Object type " + whichInstance.getClass().getName() + " created");
            }
        }

    }

    public static void dumpRetainedReferences() {
        try {
            Runtime.getRuntime().gc();
            Runtime.getRuntime().runFinalization();
            Thread.sleep(1000);
            Runtime.getRuntime().gc();
            Runtime.getRuntime().runFinalization();
            Thread.sleep(1000);
            Runtime.getRuntime().gc();
            Runtime.getRuntime().runFinalization();
            Thread.sleep(1000);
            Runtime.getRuntime().gc();
            Runtime.getRuntime().runFinalization();
            Thread.sleep(1000);
            Runtime.getRuntime().runFinalization();
        } catch (InterruptedException ie) {
            Defeat.noop();
        }

        if (instanceCounts.size() ==0) {
            Log.w("SingleInstanceChecker", "No instanced held! Well done!");
        }
        for (Object i: instanceCounts.keySet()) {
            Log.w("SingleInstanceChecker", " Reference to class " +i.getClass() + "/" + i + " held");
        }
    }
}
