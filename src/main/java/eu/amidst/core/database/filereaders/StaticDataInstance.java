package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.header.Variable;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataInstance implements DataInstance {

    DataRow dataRow;
    int sampleID;

    public StaticDataInstance(DataRow dataRow_, int sampleID_){
        dataRow=dataRow_;
        this.sampleID = sampleID_;
    }

    @Override
    public double getValue(Variable var) {
        return dataRow.getValue(var.getAttribute());
    }

    @Override
    public int getSampleID() {
        return sampleID;
    }

    @Override
    public int getTimeID() {
        throw new UnsupportedOperationException("Invoking getTimeID() from an data instance of static data base.");
    }
}
