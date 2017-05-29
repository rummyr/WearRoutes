package uk.me.ponies.wearroutes.tracksimplifaction;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Created by rummy on 08/06/2016.
 * Based on https://github.com/hgoebl/simplify-java/blob/master/src/main/java/com/goebl/simplify/AbstractSimplify.java
 */
public class SimplifyV2 {
    private static boolean LOW_Q = false;
    private static double LL_SCALE = 1E6; // algorithm cant handle < 1!

    public static List<LatLng> simplify(List<LatLng> data, double tolerance, int maxPoints) {
        if (data == null || data.size() <= 2) {
            return data;
        }
        double sqTolerance = tolerance * tolerance * LL_SCALE *LL_SCALE;
        List<LatLng> points;
        if (LOW_Q) {
            points = simplifyRadialDistance(data, sqTolerance);
        }
        points = simplifyDouglasPeucker(data, sqTolerance, maxPoints);
        return points;
    }
    static List<LatLng> simplifyRadialDistance(List<LatLng> points, double sqTolerance) {
        LatLng point = null;
        LatLng prevPoint = points.get(0);

        List<LatLng> newPoints = new ArrayList<LatLng>();
        newPoints.add(prevPoint);

        for (int i = 1; i < points.size(); ++i) {
            point = points.get(i);

            if (getSquareDistance(point, prevPoint) > sqTolerance) {
                newPoints.add(point);
                prevPoint = point;
            }
        }

        if (prevPoint != point) {
            newPoints.add(point);
        }

        return newPoints;
    }

    static List<LatLng> simplifyDouglasPeucker(List<LatLng> points, double sqTolerance, int maxPoints) {

        BitSet bitSet = new BitSet(points.size());
        bitSet.set(0);
        bitSet.set(points.size() - 1);

        List<RangeXTE> stack = new ArrayList<RangeXTE>();
        // add in the first uninitialized XTE segment .. goes all the way across, is uninitialized!
        RangeXTE initialRangeXTE = new RangeXTE(0, points.size() - 1);
        initRangeXTE(initialRangeXTE, points);
        stack.add(initialRangeXTE);

        while (!stack.isEmpty() && bitSet.cardinality() < maxPoints) {

            // find the range with the largest deviation .. and work on that one
            // Range range = stack.remove(stack.size() - 1);
            RangeXTE worstRange = findWorstSegment(stack, points);
            // Log.d("SimplifyV2", "worstRange is " + worstRange);

            if (worstRange.xteOfFurthestPoint > sqTolerance) {
                // add this point to the set of included points
                bitSet.set(worstRange.indexOfFurthestPoint);

                // split the range we just pulled
                stack.remove(worstRange);
                RangeXTE r1 = new RangeXTE(worstRange.indexOfFirstPoint, worstRange.indexOfFurthestPoint);
                initRangeXTE(r1,points);
                RangeXTE r2 = new RangeXTE(worstRange.indexOfFurthestPoint, worstRange.indexOfLastPoint);
                initRangeXTE(r2,points);
                stack.add(r1);
                stack.add(r2);
                //Log.d("SimplifyV2", "split " + worstRange);
                //Log.d("SimplifyV2", "into " + r1);
                //Log.d("SimplifyV2", "and " + r2);
            }
        }

        List<LatLng> newPoints = new ArrayList<LatLng>(bitSet.cardinality());
        for (int index = bitSet.nextSetBit(0); index >= 0; index = bitSet.nextSetBit(index + 1)) {
            newPoints.add(points.get(index));
        }

        return newPoints;
    }

    static public double getSquareDistance(LatLng p1, LatLng p2) {

        double dx = p1.latitude - p2.latitude;
        double dy = p1.longitude - p2.longitude;

        return (dx * dx + dy * dy) * LL_SCALE*LL_SCALE;
    }

    static public double getSquareSegmentDistance(LatLng p0, LatLng p1, LatLng p2) {
        double x0, y0, x1, y1, x2, y2, dx, dy, t;

        x1 = p1.latitude;
        y1 = p1.longitude;
        x2 = p2.latitude;
        y2 = p2.longitude;
        x0 = p0.latitude;
        y0 = p0.longitude;

        dx = x2 - x1;
        dy = y2 - y1;

        if (dx != 0.0d || dy != 0.0d) {
            t = ((x0 - x1) * dx + (y0 - y1) * dy)
                    / (dx * dx + dy * dy);

            if (t > 1.0d) {
                x1 = x2;
                y1 = y2;
            } else if (t > 0.0d) {
                x1 += dx * t;
                y1 += dy * t;
            }
        }

        dx = x0 - x1;
        dy = y0 - y1;

        return (dx * dx + dy * dy)*LL_SCALE*LL_SCALE;
    }


    static void initRangeXTE(RangeXTE r, List<LatLng> points) {
        if (r.inited) {
            return;
        }
        double maxSqDist = 0f;
        int indexOfPointWithMaxSqDist;
        for (int i = r.indexOfFirstPoint + 1; i < r.indexOfLastPoint; ++i) { // for each point within range
            double sqDist = getSquareSegmentDistance(points.get(i), points.get(r.indexOfFirstPoint), points.get(r.indexOfLastPoint));

            if (sqDist > r.xteOfFurthestPoint) {
                r.xteOfFurthestPoint = sqDist;
                r.indexOfFurthestPoint = i;
                r.inited = true;
            }
        } // end for each possible point in range
    }

    static RangeXTE findWorstSegment(List<RangeXTE> stack, List<LatLng> points) {
        double maxRangeSqDist = 0;
        RangeXTE rangeWithMaxXTE = null;

        for (int rangeIndex = 0; rangeIndex< stack.size();rangeIndex++) { // for each possible range
            //TODO: optimizations very very available here!
            RangeXTE r = stack.get(rangeIndex);
            initRangeXTE(r, points); // should quickly exit!
            // now see if this is the worst range
            if (r.xteOfFurthestPoint > maxRangeSqDist) {
                rangeWithMaxXTE = r;
                maxRangeSqDist = r.xteOfFurthestPoint;
            }
        } // end for each possible range
        return rangeWithMaxXTE;

    }

    static class RangeXTE {
        public boolean inited = false;
        public double xteOfFurthestPoint;
        public int indexOfFurthestPoint;
        public final int indexOfFirstPoint;
        public final int indexOfLastPoint;

        public RangeXTE(int indexOfFirstPoint, int indexOfLastPoint) {
            this.indexOfFirstPoint = indexOfFirstPoint;
            this.indexOfLastPoint = indexOfLastPoint;
        }

        public String toString() {
            String rv = "RangeXTE covering " + indexOfFirstPoint + ".." + indexOfLastPoint;
            if (inited) {
                rv += " Initialized, furthestPoint is " + indexOfFurthestPoint + " dist:" + xteOfFurthestPoint;
            }
            return rv;
        }
    }

}



