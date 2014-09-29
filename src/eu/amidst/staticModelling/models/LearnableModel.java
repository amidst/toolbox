package eu.amidst.staticModelling.models;

import eu.amidst.core.database.statics.DataInstance;
import eu.amidst.core.database.statics.DataStream;
import eu.amidst.core.datastructures.statics.BayesianNetwork;
import eu.amidst.core.header.statics.StaticDataHeader;
import eu.amidst.core.potential.Potential;
import eu.amidst.staticModelling.learning.LearningAlgorithm;
import eu.amidst.staticModelling.learning.MaximumLikelihood;

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
