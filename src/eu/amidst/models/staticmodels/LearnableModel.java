package eu.amidst.models.staticmodels;

import eu.amidst.core.Potential.Potential;
import eu.amidst.core.StaticBayesianNetwork.BayesianNetwork;
import eu.amidst.core.StaticDataBase.DataInstance;
import eu.amidst.core.StaticDataBase.DataStream;
import eu.amidst.core.headers.StaticDataHeader;
import eu.amidst.learning.staticLearning.LearningAlgorithm;
import eu.amidst.learning.staticLearning.MaximumLikelihood;

/**
 * Created by andresmasegosa on 28/08/14.
 */
public abstract class LearnableModel {

    BayesianNetwork bnet;

    LearningAlgorithm algorithm = new MaximumLikelihood();

    public abstract void buildStructure(StaticDataHeader dataHeader);

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
        if (data.getDataStream().getStaticDataHeader()!=this.getBayesianNetwork().getStaticModelHeader().getStaticDataHeader())
            return;

        algorithm.updateModel(data);
    }

    public void learnModelFromStream(DataStream dataStream){
        algorithm.learnModelFromStream(dataStream);
    }


    private void setLeaves(){

    }

}
