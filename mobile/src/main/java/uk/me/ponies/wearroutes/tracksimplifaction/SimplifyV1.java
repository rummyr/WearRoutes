package uk.me.ponies.wearroutes.tracksimplifaction;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Created by rummy on 08/06/2016.
 * Based on https://github.com/hgoebl/simplify-java/blob/master/src/main/java/com/goebl/simplify/AbstractSimplify.java
 */
public class SimplifyV1 {
    private static boolean LOW_Q = false;
    private static double LL_SCALE = 1E6; // algorithm cant handle < 1!

    public static List<LatLng> simplify(List<LatLng> data, double tolerance) {
        if (data == null || data.size() <= 2) {
            return data;
        }
        double sqTolerance = tolerance * tolerance * LL_SCALE *LL_SCALE;
        List<LatLng> points;
        if (LOW_Q) {
            points = simplifyRadialDistance(data, sqTolerance);
        }
        points = simplifyDouglasPeucker(data, sqTolerance);
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

    static List<LatLng> simplifyDouglasPeucker(List<LatLng> points, double sqTolerance) {

        BitSet bitSet = new BitSet(points.size());
        bitSet.set(0);
        bitSet.set(points.size() - 1);

        List<Range> stack = new ArrayList<Range>();
        stack.add(new Range(0, points.size() - 1));

        while (!stack.isEmpty()) {
            Range range = stack.remove(stack.size() - 1);

            int index = -1;
            double maxSqDist = 0f;

            // find index of point with maximum square distance from first and last point
            for (int i = range.first + 1; i < range.last; ++i) {
                double sqDist = getSquareSegmentDistance(points.get(i), points.get(range.first), points.get(range.last));

                if (sqDist > maxSqDist) {
                    index = i;
                    maxSqDist = sqDist;
                }
            }

            if (maxSqDist > sqTolerance) {
                // add this point to the set of included points
                bitSet.set(index);

                // split the range we just pulled
                stack.add(new Range(range.first, index));
                stack.add(new Range(index, range.last));
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

    private static class Range {
        private Range(int first, int last) {
            this.first = first;
            this.last = last;
        }

        int first;
        int last;
    }
}



