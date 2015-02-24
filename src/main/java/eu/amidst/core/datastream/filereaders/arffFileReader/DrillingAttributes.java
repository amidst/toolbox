package eu.amidst.core.datastream.filereaders.arffFileReader;

import com.google.common.collect.ImmutableList;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.StateSpaceType;

import java.util.List;

/**
 * Created by sigveh on 10/16/14.
 */
public class DrillingAttributes extends Attributes {

    private static final Attribute MFI = new Attribute(0, "MFI", "m3/s", StateSpaceType.REAL, 0);
    private static final Attribute SPP = new Attribute(1, "MFI", "Pa", StateSpaceType.REAL, 0);
    private static final Attribute RPM = new Attribute(2, "RPM", "1/s", StateSpaceType.REAL, 0);


    private static List<Attribute> attributesDrilling;
    {
        attributesDrilling = ImmutableList.of(MFI, SPP, RPM);
    }

    public DrillingAttributes(){
        super(attributesDrilling);
    }

    public Attribute getMFI() {
        return MFI;
    }

    public Attribute getRPM() {
        return RPM;
    }

    public Attribute getSPP() {
        return SPP;
    }

    @Override
    public List<Attribute> getList() {
        return attributesDrilling;
    }

    @Override
    public void print() {

    }

    @Override
    public Attribute getAttributeByName(String name) {
        return null;
    }


}
