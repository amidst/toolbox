package eu.amidst.core.datastream.statics.readers.impl;

import com.google.common.collect.ImmutableList;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.StateSpaceTypeEnum;

import java.util.List;


/**
 * Created by sigveh on 10/16/14.
 */
public class ForTesting4Attributes extends Attributes {

    private final Attribute CLASS = new Attribute(0, "CLASS", "NA", StateSpaceTypeEnum.FINITE_SET, 2);
    private final Attribute TWO_NAMES = new Attribute(1, "TWO NAMES", "NA", StateSpaceTypeEnum.REAL, 0);
    private final Attribute THREE_NAMES_HERE = new Attribute(1, "THREE NAMES HERE", "NA", StateSpaceTypeEnum.REAL, 0);

    private static List<Attribute> attributesTesting4;
    {
        attributesTesting4 = ImmutableList.of(CLASS, TWO_NAMES, THREE_NAMES_HERE);
    }

    public ForTesting4Attributes(){
        super(attributesTesting4);
    }

    @Override
    public List<Attribute> getList(){
        return attributesTesting4;
    }

    @Override
    public Attribute getAttributeByName(String name) {
        return null;
    }
}
