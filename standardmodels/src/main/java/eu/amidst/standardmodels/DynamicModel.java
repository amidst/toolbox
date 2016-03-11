package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.learning.dynamic.DynamicBayesianLearningAlgorithm;
import eu.amidst.dynamic.learning.dynamic.DynamicSVB;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

/**
 * Created by ana@cs.aau.dk on 04/03/16.
 */
public abstract class DynamicModel {

    DynamicBayesianLearningAlgorithm learningAlgorithm;

    protected DynamicDAG dynamicDAG;

    protected DynamicVariables variables;

    protected int windowSize = 100;

    public DynamicModel(Attributes attributes) {
        this.variables = new DynamicVariables(attributes);
        this.isValidConfiguration();
    }

    public DynamicDAG getDynamicDAG() {
        if (dynamicDAG==null){
            buildDAG();
        }
        return dynamicDAG;
    }

    public void setLearningAlgorithm(DynamicBayesianLearningAlgorithm learningAlgorithm) {
        this.learningAlgorithm = learningAlgorithm;
    }

    public void setWindowSize(int windowSize){
        this.windowSize = windowSize;
        learningAlgorithm = null;
    }

    public void learnModel(DataOnMemory<DynamicDataInstance> dataBatch){
        learningAlgorithm = new DynamicSVB();
        learningAlgorithm.setDynamicDAG(this.getDynamicDAG());
        learningAlgorithm.initLearning();
        learningAlgorithm.updateModel(dataBatch);
    }

    public void learnModel(DataStream<DynamicDataInstance> dataStream){
        learningAlgorithm = new DynamicSVB();
        learningAlgorithm.setDynamicDAG(this.getDynamicDAG());
        learningAlgorithm.setDataStream(dataStream);
        learningAlgorithm.runLearning();
    }

    public void updateModel(DataOnMemory<DynamicDataInstance> dataBatch){
        if (learningAlgorithm ==null) {
            learningAlgorithm = new DynamicSVB();
            learningAlgorithm.setDynamicDAG(this.getDynamicDAG());
            ((DynamicSVB)learningAlgorithm).setWindowsSize(windowSize);
            learningAlgorithm.initLearning();
        }

        learningAlgorithm.updateModel(dataBatch);
    }


    public DynamicBayesianNetwork getModel(){
        if (learningAlgorithm !=null){
            return this.learningAlgorithm.getLearntDBN();
        }

        return null;
    }



    protected abstract void buildDAG();

    public abstract void isValidConfiguration();

    @Override
    public String toString() {
        return this.getModel().toString();
    }
}
