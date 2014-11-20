package eu.amidst.core.variables;

/**
 * Created by sigveh on 10/20/14.
 */
public enum StateSpaceType {REAL, MULTINOMIAL;

        public static StateSpaceType parseKind(String s) {
            s = s.toUpperCase();
            if(s.startsWith("{") && s.endsWith("}")){
                return MULTINOMIAL;
            }

            switch (s) {
                case "REAL":
                    return REAL;
                default:
                    throw new IllegalArgumentException(" The string \"" + s + "\" does not map to Kind.");
            }
        }
    }
