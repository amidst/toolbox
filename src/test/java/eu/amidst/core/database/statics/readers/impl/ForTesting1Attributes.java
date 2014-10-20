package eu.amidst.core.database.statics.readers.impl;

import com.google.common.collect.ImmutableSet;
import eu.amidst.core.database.statics.readers.Attribute;
import eu.amidst.core.database.statics.readers.Attributes;
import eu.amidst.core.database.statics.readers.Kind;

import java.util.Set;

/**
 * Created by sigveh on 10/16/14.
 */
public class ForTesting1Attributes implements Attributes {

    private final Attribute CLASS = new Attribute(0, "CLASS", "NA", Kind.INTEGER);
    private final Attribute F1 = new Attribute(0, "F1", "NA", Kind.REAL);
    private final Attribute F2 = new Attribute(1, "F2", "NA", Kind.REAL);
    private final Attribute F3 = new Attribute(2, "F3", "NA", Kind.REAL);
    private final Attribute F4 = new Attribute(3, "F4", "NA", Kind.REAL);
    private final Attribute F5 = new Attribute(4, "F5", "NA", Kind.REAL);
    private final Attribute F6 = new Attribute(5, "F6", "NA", Kind.REAL);
    private final Attribute F7 = new Attribute(6, "F7", "NA", Kind.REAL);
    private final Attribute F8 = new Attribute(7, "F8", "NA", Kind.REAL);
    private final Attribute F9 = new Attribute(8, "F9", "NA", Kind.REAL);


    private static Set<Attribute> attributes;
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


    public Attribute getCLASS() {
        return CLASS;
    }

    public Attribute getF1() {
        return F1;
    }

    public Attribute getF2() {
        return F2;
    }

    public Attribute getF3() {
        return F3;
    }

    public Attribute getF4() {
        return F4;
    }

    public Attribute getF5() {
        return F5;
    }

    public Attribute getF6() {
        return F6;
    }

    public Attribute getF7() {
        return F7;
    }

    public Attribute getF8() {
        return F8;
    }

    public Attribute getF9() {
        return F9;
    }

}
