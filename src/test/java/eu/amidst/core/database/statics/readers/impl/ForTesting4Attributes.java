package eu.amidst.core.database.statics.readers.impl;

import com.google.common.collect.ImmutableSet;
import eu.amidst.core.database.statics.readers.Attribute;
import eu.amidst.core.database.statics.readers.Attributes;
import eu.amidst.core.database.statics.readers.StateSpaceType;

import java.util.Set;

/**
 * Created by sigveh on 10/16/14.
 */
public class ForTesting4Attributes implements Attributes {

    private final Attribute CLASS = new Attribute(0, "CLASS", "NA", StateSpaceType.INTEGER);
    private final Attribute TWO_NAMES = new Attribute(1, "TWO NAMES", "NA", StateSpaceType.REAL);
    private final Attribute THREE_NAMES_HERE = new Attribute(1, "THREE NAMES HERE", "NA", StateSpaceType.REAL);

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
