package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.header.Variable;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataInstance implements DataInstance {

    private DataRow dataRow;
    public StaticDataInstance(DataRow dataRow_){
        dataRow=dataRow_;
    }

    @Override
    public double getValue(Variable var) {
        return dataRow.getValue(var.getAttribute());
    }

    @Override
    public int getSequenceID() {
        throw new UnsupportedOperationException("Invoking getSequenceID() from an data instance of static data base.");
    }

    @Override
    public int getTimeID() {
        throw new UnsupportedOperationException("Invoking getTimeID() from an data instance of static data base.");
    }
}
