package eu.amidst.moalink.converterFromMoaToAmidst;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.filereaders.DataRow;
import weka.core.Instance;

/**
 * Created by ana@cs.aau.dk on 14/11/14.
 */
public class DataRowWeka implements DataRow {

    private Instance dataRow;
    private Attributes attributes;


    public DataRowWeka(Instance dataRow, Attributes attributes_){
            this.dataRow = dataRow;
            this.attributes = attributes_;
    }

    @Override
    public double getValue(Attribute att) {
        return dataRow.value(att.getIndex());
    }

    @Override
    public void setValue(Attribute att, double value) {
        dataRow.setValue(att.getIndex(), value);
    }

    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    @Override
    public double[] toArray() {
        return dataRow.toDoubleArray();
    }
}
