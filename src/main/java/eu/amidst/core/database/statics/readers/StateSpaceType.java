package eu.amidst.core.database.statics.readers;

/**
 * Created by sigveh on 10/20/14.
 */
public enum StateSpaceType {REAL, INTEGER;

        public static StateSpaceType parseKind(String s) {
            s = s.toUpperCase();
            if(s.startsWith("{") && s.endsWith("}")){
                return INTEGER;
            }

            switch (s) {
                case "REAL":
                    return REAL;
                default:
                    throw new IllegalArgumentException(" The string \"" + s + "\" does not map to Kind.");
            }
        }
    }
