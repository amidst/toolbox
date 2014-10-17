package eu.amidst.core.database.statics.readers.Impl;

import com.google.common.collect.ImmutableSet;
import eu.amidst.core.database.Attributes;

import java.util.Set;

/**
 * Created by sigveh on 10/16/14.
 */
public class ForTesting2Attributes implements Attributes {

    private final Attribute CLASS = new Attribute(0, "CLASS", "NA", Kind.DISCRETE);
    private final Attribute TWO_NAMES = new Attribute(1, "TWO NAMES", "NA", Kind.REAL);
    private final Attribute THREE_NAMES_HERE = new Attribute(2, "THREE NAMES HERE", "NA", Kind.REAL);


    private static Set<Attribute> attributes;
    {
        attributes = ImmutableSet.of(CLASS, TWO_NAMES, THREE_NAMES_HERE);
    }

    @Override
    public Set<Attribute> getSet(){
        return attributes;
    }

    @Override
    public void print() {

    }
}
