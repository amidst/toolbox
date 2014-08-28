package eu.amidst.models.staticmodels;

import eu.amidst.core.Potential.ConstantPotential;
import eu.amidst.core.Potential.Potential;
import eu.amidst.core.Potential.PotentialTable;
import eu.amidst.core.StaticBayesianNetwork.BNFactory;
import eu.amidst.core.StaticBayesianNetwork.BayesianNetwork;
import eu.amidst.core.StaticDataBase.DataInstance;
import eu.amidst.core.headers.StaticDataHeader;
import eu.amidst.core.headers.StaticModelHeader;
import eu.amidst.core.utils.Utils;
import eu.amidst.learning.staticLearning.MaximumLikelihood;

/**
 * Created by afa on 02/07/14.
 */
public class NaiveBayes extends LearnableModel implements Classifier {

    int classID=0;

    public NaiveBayes(){
        this.setLearningAlgorithm(new MaximumLikelihood());
    }

    @Override
    public double[] predict(DataInstance data) {

        if (!Utils.isMissing(data.getValue(this.getClassVarID())))
            return null;

        PotentialTable potResult = new PotentialTable(this.getBayesianNetwork().getVariable(this.classID).getNumberOfStates());


        for (int i=0; i<this.getBayesianNetwork().getNumberOfNodes(); i++) {
            if (Utils.isMissing(data.getValue(i)))
                continue;
            Potential pot = this.getBayesianNetwork().getEstimator(i).getRestrictedPotential(data);
            potResult.combine(pot);
        }

        return potResult.getValues();
    }

    @Override
    public int getClassVarID() {
        return classID;
    }

    @Override
    public void setClassVarID(int varID) {
        this.classID=varID;
    }

    @Override
    public Potential inferenceForLearning(DataInstance data, int varID) {
        if (varID==this.getClassVarID())
            return null;//Error

        if (!Utils.isMissing(data.getValue(varID)))
            return new ConstantPotential(this.getBayesianNetwork().getEstimator(varID).getProbability(data));

        return this.getBayesianNetwork().getEstimator(varID).getRestrictedPotential(data);

    }

    @Override
    public void buildStructure(StaticDataHeader dataHeader){

        StaticModelHeader modelHeader = new StaticModelHeader(dataHeader);

        BayesianNetwork net = BNFactory.createBN(modelHeader);

        for (int i=0; i<net.getNumberOfNodes(); i++){
            if (i==this.getClassVarID())
                continue;
            net.getParentSet(i).addParent(this.getClassVarID());
        }

        net.initEstimators();

        this.bnet=net;
    }


}
