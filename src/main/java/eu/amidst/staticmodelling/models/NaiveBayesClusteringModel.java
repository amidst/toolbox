/*
package eu.amidst.staticmodelling.models;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.header.StateSpaceType;
import eu.amidst.core.header.VariableBuilder;
import eu.amidst.core.modelstructure.statics.BNFactory;
import eu.amidst.core.modelstructure.statics.BayesianNetwork;
import eu.amidst.core.header.Variable;
import eu.amidst.core.header.StaticModelHeader;
import eu.amidst.core.potential.Potential;
import eu.amidst.core.potential.PotentialTable;
import eu.amidst.core.utils.Utils;

*/
/**
 * Created by andresmasegosa on 28/08/14.
 *//*

public class NaiveBayesClusteringModel extends LearnableModel{

    int hiddenClassID;

    public int getHiddenClassID(){
        return hiddenClassID;
    }
    @Override
    public void buildStructure(Attributes atts) {

        StaticModelHeader modelHeader =  new StaticModelHeader(atts);

        VariableBuilder builder = new VariableBuilder();
        VariableBuilder.setName("H");
        VariableBuilder.setNumberOfStates(2);
        VariableBuilder.setStateSpaceType(StateSpaceType.MULTINOMIAL);

        Variable hiddenVar = modelHeader.addHiddenDynamicVariable(builder);



        this.hiddenClassID = hiddenVar.getVarID();

        BayesianNetwork net = BNFactory.createBN(modelHeader);

        for (int i=0; i<net.getNumberOfDynamicVars(); i++){
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
            return this.getBayesianNetwork().getDistribution(this.hiddenClassID).getRestrictedPotential(data);
        }

        PotentialTable potResult = new PotentialTable(this.getBayesianNetwork().getVariableByName(varID).getNumberOfStates());

        for (int i=0; i<this.getBayesianNetwork().getNumberOfDynamicVars(); i++) {
            if (i==this.getHiddenClassID())
                continue;
            if (Utils.isMissing(data.getValue(i)))
                continue;

            Potential pot = this.getBayesianNetwork().getDistribution(i).getRestrictedPotential(data);
            potResult.combine(pot);
        }

        if (Utils.isMissing(data.getValue(varID))) {
            Potential pot = this.getBayesianNetwork().getDistribution(varID).getRestrictedPotential(data);
            potResult.combine(pot);
        }

        return potResult;
    }

    public double[] clusterMemberShip(DataInstance data){

        if (!Utils.isMissing(data.getValue(this.getHiddenClassID())))
            return null;//Error

        PotentialTable potResult = (PotentialTable) this.getBayesianNetwork().getDistribution(this.getHiddenClassID()).getRestrictedPotential(data);

        for (int i=0; i<this.getBayesianNetwork().getNumberOfDynamicVars(); i++) {
            if (Utils.isMissing(data.getValue(i)) || i==this.getHiddenClassID())
                continue;
            Potential pot = this.getBayesianNetwork().getDistribution(i).getRestrictedPotential(data);
            potResult.combine(pot);
        }
        potResult.normalize();

        return potResult.getValues();
    }

}
*/
