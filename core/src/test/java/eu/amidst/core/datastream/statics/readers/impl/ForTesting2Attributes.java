package eu.amidst.core.datastream.statics.readers.impl;

import com.google.common.collect.ImmutableList;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.StateSpaceTypeEnum;

import java.util.List;

/**
 * Created by sigveh on 10/16/14.
 */
public class ForTesting2Attributes extends Attributes {

    private final Attribute CLASS = new Attribute(0, "CLASS", "NA", StateSpaceTypeEnum.FINITE_SET, 2);
    private final Attribute TWO_NAMES = new Attribute(0, "TWO NAMES", "NA", StateSpaceTypeEnum.REAL, 0);
    private final Attribute THREE_NAMES_HERE = new Attribute(1, "THREE NAMES HERE", "NA", StateSpaceTypeEnum.REAL, 0);

    private static List<Attribute> attributesTesting2;
    {
        attributesTesting2 = ImmutableList.of(CLASS, TWO_NAMES, THREE_NAMES_HERE);
    }

    public ForTesting2Attributes(){
        super(attributesTesting2);
    }

    @Override
    public List<Attribute> getList(){
        return attributesTesting2;
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
