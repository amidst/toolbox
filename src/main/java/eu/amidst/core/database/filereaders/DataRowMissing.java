package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attribute;

/**
 * Created by ana@cs.aau.dk on 13/11/14.
 */
public class DataRowMissing implements DataRow{

    @Override
    public double getValue(Attribute att) {
        return Double.NaN;
    }

    @Override
    public void setValue(Attribute att, double value) {

    }
}
