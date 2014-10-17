package eu.amidst.core.database.statics.readers.Impl;

import com.google.common.collect.ImmutableSet;
import eu.amidst.core.database.Attributes;

import java.util.List;
import java.util.Set;

/**
 * Created by sigveh on 10/16/14.
 */
public class ForTesting1Attributes implements Attributes {

    private final Attribute CLASS = new Attribute(0, "CLASS", "NA", Kind.DISCRETE);
    private final Attribute F1 = new Attribute(1, "F1", "NA", Kind.REAL);
    private final Attribute F2 = new Attribute(2, "F2", "NA", Kind.REAL);
    private final Attribute F3 = new Attribute(3, "F3", "NA", Kind.REAL);
    private final Attribute F4 = new Attribute(4, "F4", "NA", Kind.REAL);
    private final Attribute F5 = new Attribute(5, "F5", "NA", Kind.REAL);
    private final Attribute F6 = new Attribute(6, "F6", "NA", Kind.REAL);
    private final Attribute F7 = new Attribute(7, "F7", "NA", Kind.REAL);
    private final Attribute F8 = new Attribute(8, "F8", "NA", Kind.REAL);
    private final Attribute F9 = new Attribute(9, "F9", "NA", Kind.REAL);


    private static Set<Attributes.Attribute> attributes;
    {
    attributes = ImmutableSet.of(CLASS, F1, F2, F3, F4, F5, F6, F7, F8, F9);
    }

    @Override
    public Set<Attribute> getSet(){
        return attributes;
    }

    @Override
    public void print() {

    }
}
