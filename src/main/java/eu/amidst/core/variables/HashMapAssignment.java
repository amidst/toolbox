package eu.amidst.core.variables;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 03/11/14.
 */
public class HashMapAssignment implements DataInstance {
    private HashMap<Variable,Double> assignment;
    int sequenceID;
    int timeID;

    public HashMapAssignment(int nOfVars){
        assignment = new HashMap(nOfVars);
    }

    public double getValue(Variable key){
        Double val = assignment.get(key);
        if (val!=null){
            return val.doubleValue();
        }
        else {
            //throw new IllegalArgumentException("No value stored for the requested variable: "+key.getName());
            return Utils.missingValue();
        }
    }
    public void putValue(Variable var, double val) {
        this.assignment.put(var,val);
    }

    public int getSequenceID() {
        return sequenceID;
    }

    public void setSequenceID(int sequenceID) {
        this.sequenceID = sequenceID;
    }

    public int getTimeID() {
        return timeID;
    }

    public void setTimeID(int timeID) {
        this.timeID = timeID;
    }

    public void setValue(Variable var, Double value){
        assignment.put(var, value);
    }

    // Now you can use the following loop to iterate over all assignments:
    // for (Map.Entry<Variable, Double> entry : assignment.entrySet()) return entry;
    public Set<Map.Entry<Variable,Double>> entrySet(){
        return assignment.entrySet();
    }

}
