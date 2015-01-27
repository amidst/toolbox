package eu.amidst.core.database.filereaders;

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
    public double getValue(Variable var) {
        if (var.isTemporalClone()){
            return dataRowPast.getValue(var.getAttribute());
        }else {
            return dataRowPresent.getValue(var.getAttribute());
        }
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
