package de.dakror.quarry.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

import de.dakror.common.libgdx.math.Point;

// https://stackoverflow.com/a/42650092/4882174
// Adapted for libGDX
public class PolygonHelper {

    public Polygon makePolygon(Iterable<Rectangle> rectangles) {
        Array<Point> points = calcPoints(rectangles);
        if (points.isEmpty()) return null;

        float[] vertices = new float[points.size * 2];
        for (int i = 0; i < points.size; i++) {
            vertices[2 * i] = (float) (double) points.get(i).getX();
            vertices[2 * i + 1] = (float) (double) points.get(i).getY();
        }
        System.out.println(points);
        System.out.println(Arrays.toString(vertices));
        return new Polygon(vertices);
    }

    private Array<Point> calcPoints(Iterable<Rectangle> rectangles) {
        Array<Point> ret = new Array<>();

        FloatArray yCoords = new FloatArray();
        Set<Float> allY = getAllYCoords(rectangles);
        for (Float f : allY)
            yCoords.add(f);

        yCoords.sort();
        yCoords.reverse();

        float previousLeftCoord = 0;
        float previousRightCoord = 0;

        for (int i = 0; i < yCoords.size; i++) {
            float yCoord = yCoords.get(i);

            //            System.out.println("Considering yCoords " + yCoord);
            float minimumXLeftCoord = minXLeftCoord(yCoord, rectangles);
            float maximumXRightCoord = maxXRightCoord(yCoord, rectangles);
            //            System.out.println("min X: " + minimumXLeftCoord);
            //            System.out.println("max X: " + maximumXRightCoord);

            if (yCoord == yCoords.first()) {
                ret.add(new Point(minimumXLeftCoord, yCoord));
                ret.add(new Point(maximumXRightCoord, yCoord));
            } else {
                if (minimumXLeftCoord != previousLeftCoord) {
                    ret.insert(0, new Point(previousLeftCoord, yCoord));
                    ret.insert(0, new Point(minimumXLeftCoord, yCoord));
                } else {
                    ret.insert(0, new Point(minimumXLeftCoord, yCoord));
                }

                if (maximumXRightCoord != previousRightCoord) {
                    ret.add(new Point(previousRightCoord, yCoord));
                    ret.add(new Point(maximumXRightCoord, yCoord));
                } else {
                    ret.add(new Point(maximumXRightCoord, yCoord));
                }

            }

            previousLeftCoord = minimumXLeftCoord;
            previousRightCoord = maximumXRightCoord;
            //            System.out.println(ret);
        }

        return ret;

    }

    private Set<Float> getAllYCoords(Iterable<Rectangle> rectangles) {
        Set<Float> allCoords = new HashSet<>();

        for (Rectangle r : rectangles) {
            allCoords.add(r.y);
            allCoords.add(r.y + r.height);
        }

        return allCoords;
    }

    private float minXLeftCoord(Float y, Iterable<Rectangle> rectangles) {
        Array<Rectangle> rects = rectanglesAtY(y, rectangles);
        float min = Float.MAX_VALUE;
        for (Rectangle r : rects) {
            min = Math.min(min, r.x);
        }

        return min;
    }

    private float maxXRightCoord(Float y, Iterable<Rectangle> rectangles) {
        Array<Rectangle> rects = rectanglesAtY(y, rectangles);
        float max = 0;
        for (Rectangle r : rects) {
            max = Math.max(max, r.x + r.width);
        }

        return max;
    }

    private Array<Rectangle> rectanglesAtY(Float y, Iterable<Rectangle> rectangles) {
        Array<Rectangle> rectsAtYExcBottomLines = rectsAtYExcBottomLines(y, rectangles);

        if (rectsAtYExcBottomLines.size > 0) {
            // there are rectangles that are not closing here, so ignore those that are closing.
            return rectsAtYExcBottomLines;
        } else {
            // there are only rectangle bottom lines so we need to consider them.
            return rectsAtYIncBottomLines(y, rectangles);
        }
    }

    private Array<Rectangle> rectsAtYExcBottomLines(Float y, Iterable<Rectangle> rectangles) {
        Array<Rectangle> rects = new Array<>();
        for (Rectangle r : rectangles) {
            if (r.y + r.height >= y && r.y < y)
                rects.add(r);
        }

        return rects;
    }

    private Array<Rectangle> rectsAtYIncBottomLines(Float y, Iterable<Rectangle> rectangles) {
        Array<Rectangle> rects = new Array<>();
        for (Rectangle r : rectangles) {
            if (r.y + r.height >= y && r.y == y)
                rects.add(r);
        }

        return rects;
    }
}
