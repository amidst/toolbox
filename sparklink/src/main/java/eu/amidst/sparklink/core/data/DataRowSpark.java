package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.filereaders.DataRow;

import java.io.Serializable;

/**
 * Created by jarias on 21/06/16.
 */
public class DataRowSpark implements DataRow, Serializable {


    private Attributes attributes;
    private double[] row;

    public DataRowSpark(double[] instance, Attributes atts) {

        row = instance;
        attributes = atts;
    }

    @Override
    public double getValue(Attribute att) {
        return row[att.getIndex()];
    }

    @Override
    public void setValue(Attribute att, double value) {
        row[att.getIndex()] = value;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public double[] toArray() {
        return row;
    }
}
