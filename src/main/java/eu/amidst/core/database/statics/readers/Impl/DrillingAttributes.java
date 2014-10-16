package eu.amidst.core.database.statics.readers.Impl;

import com.google.common.collect.ImmutableList;
import eu.amidst.core.database.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sigveh on 10/16/14.
 */
public class DrillingAttributes implements Attributes {


    private static final Attribute MFI = new Attribute(0, "MFI", "m3/s", Kind.NUMERIC);
    private static final Attribute SPP = new Attribute(1, "MFI", "Pa", Kind.NUMERIC);
    private static final Attribute RPM = new Attribute(2, "RPM", "1/s", Kind.NUMERIC);


    private static List<Attribute> attributes;
    {
        attributes = ImmutableList.of(MFI, SPP, RPM);
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
    public List<Attribute> getParameters() {
        return attributes;
    }

    @Override
    public void print() {

    }


}
