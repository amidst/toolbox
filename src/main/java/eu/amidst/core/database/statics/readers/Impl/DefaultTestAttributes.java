package eu.amidst.core.database.statics.readers.Impl;

import eu.amidst.core.database.Attributes;

/**
 * Created by sigveh on 10/16/14.
 */
public class DefaultTestAttributes implements Attributes {

    private final Attribute CL = new Attribute(0, "CL", "NA", Kind.REAL);
    private final Attribute F1 = new Attribute(1, "F1", "NA", Kind.REAL);
    private final Attribute F2 = new Attribute(2, "F2", "NA", Kind.REAL);
    private final Attribute F3 = new Attribute(3, "F3", "NA", Kind.REAL);
    private final Attribute F4 = new Attribute(4, "F4", "NA", Kind.REAL);
    private final Attribute F5 = new Attribute(5, "F5", "NA", Kind.REAL);
    private final Attribute F6 = new Attribute(6, "F6", "NA", Kind.REAL);
    private final Attribute F7 = new Attribute(7, "F7", "NA", Kind.REAL);
    private final Attribute F8 = new Attribute(8, "F8", "NA", Kind.REAL);
    private final Attribute F9 = new Attribute(9, "F9", "NA", Kind.REAL);

    @Override
    public Iterable<Attributes> getParameters() {
        return null;
    }

    @Override
    public void print() {

    }
}
