package eu.amidst.core.database.statics.readers;

/**
 * Created by sigveh on 10/20/14.
 */

public final class Attribute {

    private final int index;
    private final String unit;
    private final String name;
    private final Kind kind;

    public Attribute(int index, String name, String unit, Kind kind) {
        this.index = index;
        this.name = name.toUpperCase();
        this.unit = unit;
        this.kind = kind;
    }


    public Attribute(String name, Kind kind) {
        this.index = -1;
        this.name = name.toUpperCase();
        this.unit = "NA";
        this.kind = kind;
    }


    public int getIndex() {
        return index;
    }

    public String getUnit() {
        return unit;
    }

    public String getName() {
        return name;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (kind != attribute.kind) return false;
        if (!name.equals(attribute.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + kind.hashCode();
        return result;
    }
}
