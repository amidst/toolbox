package eu.amidst.core.database.statics.readers.impl;

import eu.amidst.core.database.statics.readers.Attribute;
import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.database.statics.readers.DataStream;

/**
 * Created by sigveh on 10/20/14.
 */
public class DefaultDataInstance implements DataInstance {
    private double[][] doubleData;
    private int[][] intData;
    private int instance;

    public DefaultDataInstance(int instance, double[][] doubleData, int[][] intData) {
        this.instance = instance;
        this.doubleData = doubleData;
        this.intData = intData;
    }

    @Override
    public double getValue(int varID) {
        //TODO remove method and replace by the next two!!!!!!!!!!!!!!!!!
        return 0;
    }

    @Override
    public double getReal(Attribute attribute) {
        return doubleData[instance][attribute.getIndex()];
    }

    @Override
    public int getInteger(Attribute attribute) {
        return intData[instance][attribute.getIndex()];
    }

    @Override
    public DataStream getDataStream() {
        //TODO remove!!!!!!!!!
        return null;
    }
}
