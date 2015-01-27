package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.StaticDataInstance;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 11/11/14.
 */
class StaticDataInstanceImpl implements StaticDataInstance {

    private DataRow dataRow;

    public StaticDataInstanceImpl(DataRow dataRow1){
        dataRow=dataRow1;
    }

    @Override
    public double getValue(Variable var) {
        return dataRow.getValue(var.getAttribute());
    }

}
