package eu.amidst.core.datastream.filereaders;

import eu.amidst.core.datastream.Attribute;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataRow {

    double getValue(Attribute att);

    void setValue(Attribute att, double value);
}


