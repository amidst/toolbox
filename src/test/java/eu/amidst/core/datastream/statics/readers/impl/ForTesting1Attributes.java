package eu.amidst.core.datastream.statics.readers.impl;

import com.google.common.collect.ImmutableList;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.StateSpaceTypeEnum;

import java.util.List;

/**
 * Created by sigveh on 10/16/14.
 */
public class ForTesting1Attributes extends Attributes {

    private final Attribute CLASS = new Attribute(0, "CLASS", "NA", StateSpaceTypeEnum.FINITE_SET, 2);
    private final Attribute F1 = new Attribute(0, "F1", "NA", StateSpaceTypeEnum.REAL, 0);
    private final Attribute F2 = new Attribute(1, "F2", "NA", StateSpaceTypeEnum.REAL, 0);
    private final Attribute F3 = new Attribute(2, "F3", "NA", StateSpaceTypeEnum.REAL, 0);
    private final Attribute F4 = new Attribute(3, "F4", "NA", StateSpaceTypeEnum.REAL, 0);
    private final Attribute F5 = new Attribute(4, "F5", "NA", StateSpaceTypeEnum.REAL, 0);
    private final Attribute F6 = new Attribute(5, "F6", "NA", StateSpaceTypeEnum.REAL, 0);
    private final Attribute F7 = new Attribute(6, "F7", "NA", StateSpaceTypeEnum.REAL, 0);
    private final Attribute F8 = new Attribute(7, "F8", "NA", StateSpaceTypeEnum.REAL, 0);
    private final Attribute F9 = new Attribute(8, "F9", "NA", StateSpaceTypeEnum.REAL, 0);


    private static List<Attribute> attributesTesting1;
    {
        attributesTesting1 = ImmutableList.of(CLASS, F1, F2, F3, F4, F5, F6, F7, F8, F9);
    }

    public ForTesting1Attributes(){
        super(attributesTesting1);
    }

    @Override
    public List<Attribute> getList(){
        return attributesTesting1;
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
