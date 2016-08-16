package uk.me.ponies.wearroutes.utils;

import android.util.Log;

import java.util.WeakHashMap;

/**
 * Test class to detect if more than once instance of something is running.
 * Ideally would be a mix-in class/annotation, but whatever
 */
public class SingleInstanceChecker {
    private static Integer ONE = new Integer(1);
    private static WeakHashMap<Class, Integer> classCounts = new WeakHashMap<>();
    private static WeakHashMap<Object, Integer> instanceCounts = new WeakHashMap<>();

    public SingleInstanceChecker(Object whichInstance) {
        if (!classCounts.containsKey(whichInstance.getClass())) {
            classCounts.put(whichInstance.getClass(), ONE);
            instanceCounts.put(whichInstance, ONE);
        }
        else {
            // check that there is only ONE of this specific Object
            if (instanceCounts.get(whichInstance) != ONE) {
                Log.e(whichInstance.getClass().getSimpleName(), " Multiple Instances of Object type " + whichInstance.getClass().getName() + " created");
            }
        }

    }
}
