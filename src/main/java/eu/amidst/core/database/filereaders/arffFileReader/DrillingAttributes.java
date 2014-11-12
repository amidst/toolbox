package eu.amidst.core.database.filereaders.arffFileReader;

import com.google.common.collect.ImmutableSet;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.header.StateSpaceType;

import java.util.Set;

/**
 * Created by sigveh on 10/16/14.
 */
public class DrillingAttributes implements Attributes {

    private static final Attribute MFI = new Attribute(0, "MFI", "m3/s", StateSpaceType.REAL);
    private static final Attribute SPP = new Attribute(1, "MFI", "Pa", StateSpaceType.REAL);
    private static final Attribute RPM = new Attribute(2, "RPM", "1/s", StateSpaceType.REAL);


    private static Set<Attribute> attributes;
    {
        attributes = ImmutableSet.of(MFI, SPP, RPM);
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
    public Set<Attribute> getSet() {
        return attributes;
    }

    @Override
    public void print() {

    }

    @Override
    public Attribute getAttributeByName(String name) {
        return null;
    }


}
