/*
package eu.amidst.staticmodelling.models;

import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.database.DataStream;
import eu.amidst.core.modelstructure.statics.BayesianNetwork;
import Potential;
import eu.amidst.staticmodelling.learning.LearningAlgorithm;
import eu.amidst.staticmodelling.learning.MaximumLikelihood;

*/
/**
 * Created by andresmasegosa on 28/08/14.
 *//*

public abstract class LearnableModel {

    BayesianNetwork bnet;

    LearningAlgorithm algorithm = new MaximumLikelihood();

    //public abstract void buildStructure(StaticDataHeader dataHeader);

    public abstract Potential inferenceForLearning(DataInstance data, int varID);

    public void setLearningAlgorithm(LearningAlgorithm algorithm){
        this.algorithm=algorithm;
    }

    public void initLearning(){
        this.setLeaves();
        algorithm.setLearnableModel(this);
        algorithm.initLearning();
    }

    public BayesianNetwork getBayesianNetwork(){
        return bnet;
    }

    public void updateModel(DataInstance data){
        //Should we check whether the data comes form where it expected to come?
        //if (data.getDataStream().getStaticDataHeader()!=this.getBayesianNetwork().getStaticVariables().getStaticDataHeader())
        //    return;

        algorithm.updateModel(data);
    }

    public void learnModelFromStream(DataStream dataStream){
        algorithm.learnModelFromStream(dataStream);
    }


    private void setLeaves(){

    }

}
*/
