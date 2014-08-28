package eu.amidst.models.staticmodels;

import eu.amidst.core.Potential.Potential;
import eu.amidst.core.StaticDataBase.DataInstance;
import eu.amidst.core.headers.StaticDataHeader;
import eu.amidst.core.headers.StaticModelHeader;

/**
 * Created by afa on 02/07/14.
 */
public class TAN extends LearnableModel implements Classifier {
    int classID;

    @Override
    public double[] predict(DataInstance instance) {
        return null;
    }

    @Override
    public void buildStructure(StaticDataHeader modelHeader) {

    }

    @Override
    public Potential inferenceForLearning(DataInstance data, int varID) {
        return null;
    }

    @Override
    public int getClassVarID() {
        return classID;
    }

    @Override
    public void setClassVarID(int varID) {
        this.classID=varID;
    }
}
