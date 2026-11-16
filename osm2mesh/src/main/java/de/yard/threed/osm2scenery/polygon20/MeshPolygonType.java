package de.yard.threed.osm2scenery.polygon20;

public enum MeshPolygonType {
    // Boundary also defines the background of the mesh.
    BOUNDARY(22),
    WAY(100),
    // AREA is used for areas that are not ways, e.g. water?, landuse, farmland, forest, etc.
    // main property of an area is that it can be resized/split/cut for making play for a way.
    // Not for buildings, which cannot/shouldn't be split. But for farmland, forest, etc. which can be split by a way.
    AREA(101),
    BUILDING(102),
    CONNECTOR(200);

    private final int type;

    MeshPolygonType(int type) {
        this.type = type;
    }

    public static MeshPolygonType fromDbValue(int dbValue) {
        for (MeshPolygonType t : values()) {
            if (t.getType() == dbValue) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown DB value: " + dbValue);
    }

    public Integer getType() {
        return type;
    }
}
