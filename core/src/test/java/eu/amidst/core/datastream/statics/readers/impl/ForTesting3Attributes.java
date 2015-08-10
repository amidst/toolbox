package eu.amidst.core.datastream.statics.readers.impl;

import com.google.common.collect.ImmutableList;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.StateSpaceTypeEnum;

import java.util.List;

/**
 * Created by sigveh on 10/16/14.
 */
public class ForTesting3Attributes extends Attributes {

    private final Attribute CLASS = new Attribute(0, "CLASS", "NA", StateSpaceTypeEnum.FINITE_SET, 2);
    private final Attribute TWO_NAMES = new Attribute(1, "TWO NAMES", "NA", StateSpaceTypeEnum.FINITE_SET, 0);
    private final Attribute THREE_NAMES_HERE = new Attribute(0, "THREE NAMES HERE", "NA", StateSpaceTypeEnum.REAL, 0);

    private static List<Attribute> attributesTesting3;
    {
        attributesTesting3 = ImmutableList.of(CLASS, TWO_NAMES, THREE_NAMES_HERE);
    }

    public ForTesting3Attributes(){
        super(attributesTesting3);
    }

    @Override
    public List<Attribute> getList(){
        return attributesTesting3;
    }

    @Override
    public Attribute getAttributeByName(String name) {
        return null;
    }
}
