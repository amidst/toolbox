package eu.amidst.core.database;

import java.util.List;

/**
 * Created by sigveh on 10/16/14.
 */
public interface Attributes {

    public List<Attribute> getParameters();

    public void print();

    enum Kind { ORDINAL, NUMERIC, REAL }

    public static final class Attribute {
        private final int index;
        private final String unit;
        private final String name;
        private final Kind kind;

        public Attribute(int index, String name, String unit, Kind kind) {
            this.index = index;
            this.name = name;
            this.unit = unit;
            this.kind = kind;
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
    }
}
