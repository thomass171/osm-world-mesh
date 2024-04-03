package de.yard.threed.osm2scenery.scenery;

public class FixedWidthProvider implements WidthProvider {
    private final double width;

    public FixedWidthProvider(double width) {
        this.width = width;
    }

    public double getWidth() {
        return width;
    }
}
