package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.header.Variable;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class DynamicDataInstance implements DataInstance {

    DataRow dataRowPresent;
    DataRow dataRowPast;

    int sampleID;
    int timeID;


    public DynamicDataInstance(DataRow dataRowPresent_, DataRow dataRowPast_, int sampleID_, int timeID_){
        dataRowPresent = dataRowPresent_;
        dataRowPast =  dataRowPast_;
        this.sampleID = sampleID_;
    }

    @Override
    public double getValue(Variable var) {
        if (var.isTemporalClone()){
            return dataRowPast.getValue(var.getAttribute());
        }else {
            return dataRowPresent.getValue(var.getAttribute());
        }
    }

    @Override
    public int getSequenceID() {
        return sampleID;
    }

    @Override
    public int getTimeID() {
        return timeID;
    }

}
