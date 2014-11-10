package eu.amidst.staticmodelling.models;


import eu.amidst.core.database.DataInstance;

/**
 * Created by afa on 02/07/14.
 */
public interface Classifier {
    public double[] predict(DataInstance instance);

    public int getClassVarID();

    public void setClassVarID(int varID);
}
