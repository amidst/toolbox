package eu.amidst.core.datastream.filereaders;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class DataInstanceImpl implements DataInstance {

    private DataRow dataRow;

    public DataInstanceImpl(DataRow dataRow1){
        dataRow=dataRow1;
    }

    @Override
    public double getValue(Attribute att) {
        return dataRow.getValue(att);
    }

    @Override
    public void setValue(Attribute att, double value) {
        this.dataRow.setValue(att, value);
    }

}
