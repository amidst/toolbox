package eu.amidst.staticmodelling.models;


import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.StaticDataInstance;

/**
 * Created by afa on 02/07/14.
 */
public interface Classifier {
    double[] predict(StaticDataInstance instance);

    int getClassVarID();

    void setClassVarID(int varID);
}
