package eu.amidst.core.huginlink;

import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 18/11/14.
 */
public final class Utils {

    private Utils(){
        //Not called
    }

    public static int getConditionalDistributionType(Variable amidstVar, BayesianNetwork amidstBN) {

        int type = -1;
        List<Variable> conditioningVariables = amidstBN.getDistribution(amidstVar).getConditioningVariables();

        if (amidstVar.isMultinomial()){
            return 0;
        }
        else if (amidstVar.isGaussian()) {

            boolean multinomialParents = false;
            boolean normalParents = false;

            for (Variable v : conditioningVariables) {
                if (v.isMultinomial()) {
                    multinomialParents = true;
                } else if (v.isGaussian()) {
                    normalParents = true;
                } else {
                    throw new IllegalArgumentException("Unrecognized DistributionType. ");
                }
            }
            if (normalParents && !multinomialParents) {
                return 1;
            } else if ((!normalParents & multinomialParents) || (conditioningVariables.size() == 0)) {
                return 2;
            } else if (normalParents & multinomialParents) {
                return 3;
            } else {
                throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
        return type;
    }




    public static BayesianNetwork DBNToBN(DynamicBayesianNetwork dynamicBayesianNetwork){
        List<Variable> areParents = new ArrayList<>();
        DynamicVariables dynamicVariables = dynamicBayesianNetwork.getDynamicVariables();
        DynamicDAG dynamicDAG = dynamicBayesianNetwork.getDynamicDAG();

        for (Variable dyVar: dynamicVariables) {
            if (!areParents.contains(dyVar)) {
                areParents.add(dyVar);
            }
            for (Variable dyParent : dynamicDAG.getParentSetTimeT(dyVar)) {
                if (!areParents.contains(dyParent)) {
                    areParents.add(dyParent);
                }
            }
        }

        StaticVariables staticVariables = new StaticVariables();
        for (Variable dyVar: areParents){
            VariableBuilder builder = VariableToVariableBuilder(dyVar);
            staticVariables.addHiddenVariable(builder);
        }

        DAG dag = new DAG(staticVariables);

        for (Variable dyVar: dynamicVariables){
            Variable staticVar = staticVariables.getVariableByName(dyVar.getName());
            for (Variable dyParent : dynamicDAG.getParentSetTimeT(dyVar)){
                Variable staticParent = staticVariables.getVariableByName(dyParent.getName());
                dag.getParentSet(staticVar).addParent(staticParent);
            }
        }


        BayesianNetwork bayesianNetwork = BayesianNetwork.newBayesianNetwork(dag);


        return bayesianNetwork;
    }

    public static VariableBuilder VariableToVariableBuilder(Variable variable){
        VariableBuilder builder = new VariableBuilder();

        builder.setName(variable.getName());
        int nstates=0;
        if (variable.getNumberOfStates()<0) {
            nstates = 2;
        }
        else {
            nstates = variable.getNumberOfStates();
        }

        builder.setStateSpace(new FiniteStateSpace(nstates));
        builder.setDistributionType(DistType.MULTINOMIAL);

        /*
        builder.setStateSpace(variable.getStateSpace());
        switch (variable.getStateSpace().getStateSpaceType()) {
            case REAL:
                builder.setDistributionType(DistType.GAUSSIAN);
                break;
            case FINITE_SET:
                builder.setDistributionType(DistType.MULTINOMIAL);
                break;
            default:
                throw new IllegalArgumentException(" The string \"" + variable.getStateSpace() + "\" does not map to any Type.");
        }
        */
        return builder;
    }


    //*********************************************************************************
//            //Simulate a sample from a Hugin network
//            int nsamples = 100;
//            for (int j=0;j< nodeList.size();j++) {
//                System.out.print(((Node)nodeList.get(j)).getName());
//                if(j<nodeList.size()-1)
//                    System.out.print(",");
//            }
//            System.out.println();
//            for (int i=0;i<nsamples;i++){
//                domain.simulate();
//                for (int j=0;j<nodeList.size();j++){
//                    System.out.print(((ContinuousChanceNode)nodeList.get(j)).getSampledValue());
//                    if(j<nodeList.size()-1)
//                        System.out.print(",");
//                }
//                System.out.println();
//            }
//            //*********************************************************************************


}
