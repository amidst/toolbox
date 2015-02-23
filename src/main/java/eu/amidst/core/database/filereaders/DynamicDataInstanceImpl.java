package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DynamicDataInstance;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 11/11/14.
 */
class DynamicDataInstanceImpl implements DynamicDataInstance {

    private DataRow dataRowPresent;
    private DataRow dataRowPast;

    private int sequenceID;
    /**
     * The timeID of the Present
     */
    private int timeID;


    public DynamicDataInstanceImpl(DataRow dataRowPast1, DataRow dataRowPresent1, int sequenceID1, int timeID1){
        dataRowPresent = dataRowPresent1;
        dataRowPast =  dataRowPast1;
        this.sequenceID = sequenceID1;
        this.timeID = timeID1;
    }

    @Override
    public double getValue(Attribute att, boolean present) {
        if (present){
            return dataRowPresent.getValue(att);
        }else {
            return dataRowPast.getValue(att);
        }
    }

    @Override
    public void setValue(Attribute att, double value, boolean present) {
        if (present){
            dataRowPresent.setValue(att, value);
        }else {
            dataRowPast.setValue(att, value);
        }
    }

    @Override
    public double getValue(Attribute att) {
        return this.dataRowPresent.getValue(att);
    }

    @Override
    public void setValue(Attribute att, double val) {
        this.dataRowPresent.setValue(att,val);
    }

    @Override
    public int getSequenceID() {
        return sequenceID;
    }

    @Override
    public int getTimeID() {
        return timeID;
    }

}
