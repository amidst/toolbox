package eu.amidst.models.staticmodels;

import eu.amidst.core.Potential.Potential;
import eu.amidst.core.Potential.PotentialTable;
import eu.amidst.core.StaticBayesianNetwork.BNFactory;
import eu.amidst.core.StaticBayesianNetwork.BayesianNetwork;
import eu.amidst.core.headers.StaticDataHeader;
import eu.amidst.core.headers.StaticModelHeader;
import eu.amidst.core.StaticDataBase.*;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.headers.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 28/08/14.
 */
public class NaiveBayesClusteringModel extends LearnableModel{

    int hiddenClassID;

    public int getHiddenClassID(){
        return hiddenClassID;
    }
    @Override
    public void buildStructure(StaticDataHeader dataHeader) {

        StaticModelHeader modelHeader =  new StaticModelHeader(dataHeader);

        Variable hiddenVar = modelHeader.addHiddenVariable("ClusterVar",2);

        this.hiddenClassID = hiddenVar.getVarID();

        BayesianNetwork net = BNFactory.createBN(modelHeader);

        for (int i=0; i<net.getNumberOfNodes(); i++){
            if (i==this.getHiddenClassID())
                continue;
            net.getParentSet(i).addParent(this.getHiddenClassID());
        }

        net.initEstimators();

        this.bnet=net;

    }

    @Override
    public Potential inferenceForLearning(DataInstance data, int varID) {

        if (varID==this.getHiddenClassID()) {
            return this.getBayesianNetwork().getEstimator(this.hiddenClassID).getRestrictedPotential(data);
        }

        PotentialTable potResult = new PotentialTable(this.getBayesianNetwork().getVariable(varID).getNumberOfStates());

        for (int i=0; i<this.getBayesianNetwork().getNumberOfNodes(); i++) {
            if (i==this.getHiddenClassID())
                continue;
            if (Utils.isMissing(data.getValue(i)))
                continue;

            Potential pot = this.getBayesianNetwork().getEstimator(i).getRestrictedPotential(data);
            potResult.combine(pot);
        }

        if (Utils.isMissing(data.getValue(varID))) {
            Potential pot = this.getBayesianNetwork().getEstimator(varID).getRestrictedPotential(data);
            potResult.combine(pot);
        }

        return potResult;
    }

    public double[] clusterMemberShip(DataInstance data){

        if (!Utils.isMissing(data.getValue(this.getHiddenClassID())))
            return null;//Error

        PotentialTable potResult = new PotentialTable(this.getBayesianNetwork().getVariable(this.getHiddenClassID()).getNumberOfStates());

        for (int i=0; i<this.getBayesianNetwork().getNumberOfNodes(); i++) {
            if (Utils.isMissing(data.getValue(i)))
                continue;
            Potential pot = this.getBayesianNetwork().getEstimator(i).getRestrictedPotential(data);
            potResult.combine(pot);
        }

        return potResult.getValues();
    }

}
