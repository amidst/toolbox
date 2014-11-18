package eu.amidst.core.database.statics.readers.impl;

import com.google.common.collect.ImmutableSet;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.header.StateSpaceType;

import java.util.Set;

/**
 * Created by sigveh on 10/16/14.
 */
public class ForTesting2Attributes extends Attributes {

    private final Attribute CLASS = new Attribute(0, "CLASS", "NA", StateSpaceType.MULTINOMIAL, 2);
    private final Attribute TWO_NAMES = new Attribute(0, "TWO NAMES", "NA", StateSpaceType.REAL, 0);
    private final Attribute THREE_NAMES_HERE = new Attribute(1, "THREE NAMES HERE", "NA", StateSpaceType.REAL, 0);

    private static Set<Attribute> attributesTesting2;
    {
        attributesTesting2 = ImmutableSet.of(CLASS, TWO_NAMES, THREE_NAMES_HERE);
    }

    public ForTesting2Attributes(){
        super(attributesTesting2);
    }

    @Override
    public Set<Attribute> getSet(){
        return attributesTesting2;
    }

    @Override
    public void print() {

    }

    @Override
    public Attribute getAttributeByName(String name) {
        return null;
    }


    public Attribute getCLASS() {
        return CLASS;
    }

    public Attribute getTWO_NAMES() {
        return TWO_NAMES;
    }

    public Attribute getTHREE_NAMES_HERE() {
        return THREE_NAMES_HERE;
    }

}
