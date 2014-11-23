package eu.amidst.core.variables;

import eu.amidst.core.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by ana@cs.aau.dk on 03/11/14.
 */
public class HashMapAssignment {
    private HashMap<Variable,Double> assignment_;

    public HashMapAssignment(int nOfVars){
        assignment_ = new HashMap(nOfVars);
    }

    public double getValue(Variable key){
        Double val = assignment_.get(key);
        if (val!=null)
            return val.doubleValue();
        else
            return Utils.missingValue();
    }

    public void setValue(Variable var, Double value){
        assignment_.put(var,value);
    }

    // Now you can use the following loop to iterate over all assignments:
    // for (Map.Entry<Variable, Double> entry : assignment_.entrySet()) return entry;
    public Set<Map.Entry<Variable,Double>> entrySet(){
        return assignment_.entrySet();
    }

}
