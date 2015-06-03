/*
package eu.amidst.staticmodelling.models;


import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.modelstructure.statics.BNFactory;
import eu.amidst.core.modelstructure.statics.BayesianNetwork;
import eu.amidst.core.header.StaticModelHeader;
import Potential;
import PotentialTable;
import Utils;
import eu.amidst.staticmodelling.learning.MaximumLikelihood;

*/
/**
 * Created by afa on 02/07/14.
 *//*

public class NaiveBayesClassifier extends LearnableModel implements Classifier {

    int classID = 0;

    public NaiveBayesClassifier() {
        this.setLearningAlgorithm(new MaximumLikelihood());
    }

    @Override
    public double[] predict(DataInstance data) {
        double currentClass = data.getValue(this.getClassVarID());

        PotentialTable potResult = (PotentialTable) this.getBayesianNetwork().getNormalDistributions(this.classID).getRestrictedPotentialExceptFor(data,this.getClassVarID());


        for (int i = 0; i < this.getBayesianNetwork().getNumberOfDynamicVars(); i++) {
            if (Utils.isMissingValue(data.getValue(i)) || i==this.getClassVarID())
                continue;
            Potential pot = this.getBayesianNetwork().getNormalDistributions(i).getRestrictedPotential(data);
            potResult.combine(pot);
        }
        potResult.normalize();

        return potResult.getValues();
    }

    @Override
    public int getClassVarID() {
        return classID;
    }

    @Override
    public void setClassVarID(int varID) {
        this.classID = varID;
    }

    @Override
    public Potential inferenceForLearning(DataInstance data, int varID) {
        return null;
    }

    @Override
    public void buildStructure(Attributes atts) {

        StaticModelHeader modelHeader = new StaticModelHeader(atts);

        BayesianNetwork net = BNFactory.createBN(modelHeader);

        for (int i = 0; i < net.getNumberOfDynamicVars(); i++) {
            if (i == this.getClassVarID())
                continue;
            net.getParentSet(i).addParent(this.getClassVarID());
        }

        net.initEstimators();

        this.bnet = net;
    }


}
*/
