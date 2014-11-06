package eu.amidst.core.distribution;

import com.google.common.collect.ImmutableSet;
import eu.amidst.core.database.statics.readers.Attribute;
import eu.amidst.core.database.statics.readers.Attributes;
import eu.amidst.core.database.statics.readers.Kind;

import java.util.Set;

public class AttributesForTesting implements Attributes {

    private final Attribute X = new Attribute(0, "X", "NA", Kind.INTEGER);
    private final Attribute Y = new Attribute(1, "Y", "NA", Kind.INTEGER);
    private final Attribute Z = new Attribute(2, "Z", "NA", Kind.INTEGER);

    private static Set<Attribute> attributes;
    {
        attributes = ImmutableSet.of(X, Y, Z);
    }

    @Override
    public Set<Attribute> getSet(){
        return attributes;
    }

    @Override
    public void print() {


    }
}
