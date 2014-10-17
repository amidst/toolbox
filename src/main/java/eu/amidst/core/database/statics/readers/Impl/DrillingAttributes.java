package eu.amidst.core.database.statics.readers.Impl;

import com.google.common.collect.ImmutableSet;
import eu.amidst.core.database.Attributes;

import java.util.List;
import java.util.Set;

/**
 * Created by sigveh on 10/16/14.
 */
public class DrillingAttributes implements Attributes {

    private static final Attribute MFI = new Attribute(0, "MFI", "m3/s", Kind.REAL);
    private static final Attribute SPP = new Attribute(1, "MFI", "Pa", Kind.REAL);
    private static final Attribute RPM = new Attribute(2, "RPM", "1/s", Kind.REAL);


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


}
