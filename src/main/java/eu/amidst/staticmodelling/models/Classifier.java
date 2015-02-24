package eu.amidst.staticmodelling.models;


import eu.amidst.core.datastream.DataInstance;

/**
 * Created by afa on 02/07/14.
 */
public interface Classifier {
    double[] predict(DataInstance instance);

    int getClassVarID();

    void setClassVarID(int varID);
}
