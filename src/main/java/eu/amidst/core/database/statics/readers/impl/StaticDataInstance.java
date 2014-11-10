package eu.amidst.core.database.statics.readers.impl;


import eu.amidst.core.database.DataInstance;
import eu.amidst.core.header.Variable;

/**
 * Created by sigveh on 10/20/14.
 */
public class StaticDataInstance implements DataInstance {
    private double[] instance;

    public StaticDataInstance(double[] instance) {
        this.instance = instance.clone();
    }

    public StaticDataInstance(StaticDataInstance copy){
        this.instance = copy.instance.clone();
    }

    @Override
    public double getValue(Variable variable) {
        return instance[variable.getVarID()];
    }

    public double[] getFullInstance(){
        return this.instance;
    }

}
