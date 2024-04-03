package de.yard.threed.osm2world;


/**
 * a texture with all information necessary for applying it to an object
 * that has texture coordinates
 */
public class TextureData {

    public static enum Wrap {REPEAT, CLAMP, CLAMP_TO_BORDER}

    /**
     * path to the texture file
     */
    /* File ist doof wegen relativem/absolutem Pfad. Einfach String wie in der Config*/
    public final String/*File*/ file;

    /**
     * width of a single tile of the texture
     */
    public final double width;

    /**
     * height of a single tile of the texture
     */
    public final double height;

    /**
     * wrap style of the texture
     */
    public final Wrap wrap;

    /**
     * calculation rule for texture coordinates
     */
    public TexCoordFunction coordFunction;

    /**
     * whether the texture is modulated with the material color.
     * Otherwise, a plain white base color is used, resulting in the texture's
     * colors appearing unaltered (except for lighting)
     */
    public final boolean colorable;

    public final boolean isBumpMap;

    // 6.5.19: Expliozites definieren von s/t ist nicht so sinnvoll, auch nicht für TextureAtlas. Lieber Segmente/Kacheln, evtl. zweidomensional
    public int segment, segments;
    public VectorXZ from = null;
    public VectorXZ to = null;
    public AtlasCell atlasCell;

    public TextureData(String/*File*/ file, double width, double height, Wrap wrap,
                       TexCoordFunction texCoordFunction, boolean colorable, boolean isBumpMap, int segment, int segments, VectorXZ from, VectorXZ to, AtlasCell atlasCell) {

        this.file = file;
        this.width = width;
        this.height = height;
        this.wrap = wrap;
        this.coordFunction = texCoordFunction;
        this.colorable = colorable;
        this.isBumpMap = isBumpMap;
        this.segment = segment;
        this.segments = segments;
        if (from != null) {
            this.from = from;
        }
        if (to != null) {
            this.to = to;
        }
        this.atlasCell = atlasCell;
    }

    //auto-generated
    @Override
    public String toString() {
        return "TextureData [file=" + file + ", width=" + width + ", height=" + height + ", wrap=" + wrap
                + ", texCoordFunction=" + coordFunction + ", colorable=" + colorable + ", isBumpMap=" + isBumpMap + "]";
    }

    // auto-generated
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (colorable ? 1231 : 1237);
        result = prime * result
                + ((coordFunction == null) ? 0 : coordFunction.hashCode());
        result = prime * result + ((file == null) ? 0 : file.hashCode());
        long temp;
        temp = Double.doubleToLongBits(height);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (isBumpMap ? 1231 : 1237);
        temp = Double.doubleToLongBits(width);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((wrap == null) ? 0 : wrap.hashCode());
        return result;
    }

    // auto-generated
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TextureData other = (TextureData) obj;
        if (colorable != other.colorable)
            return false;
        if (coordFunction == null) {
            if (other.coordFunction != null)
                return false;
        } else if (!coordFunction.equals(other.coordFunction))
            return false;
        if (file == null) {
            if (other.file != null)
                return false;
        } else if (!file.equals(other.file))
            return false;
        if (Double.doubleToLongBits(height) != Double
                .doubleToLongBits(other.height))
            return false;
        if (isBumpMap != other.isBumpMap)
            return false;
        if (Double.doubleToLongBits(width) != Double
                .doubleToLongBits(other.width))
            return false;
        if (wrap != other.wrap)
            return false;
        return true;
    }

    public static class AtlasCell {
        public int cells, x, y, w, h;

        public AtlasCell(int cells, int x, int y, int w, int h) {
            this.cells = cells;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public static AtlasCell buildFromString(String cellclause) {
            if (cellclause==null){
                return null;
            }
            String[] p = cellclause.split(",");
            int cells = Integer.parseInt(p[0]);
            int x = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            int w = Integer.parseInt(p[3]);
            int h = Integer.parseInt(p[4]);
            return new AtlasCell(cells, x, y, w, h);
        }
    }
}
