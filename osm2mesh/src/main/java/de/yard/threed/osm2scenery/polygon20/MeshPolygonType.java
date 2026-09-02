package de.yard.threed.osm2scenery.polygon20;

public enum MeshPolygonType {
    BOUNDARY(22),
    WAY(100),
    AREA(101),
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
