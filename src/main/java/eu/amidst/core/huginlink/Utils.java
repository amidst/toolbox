package eu.amidst.core.huginlink;

import COM.hugin.HAPI.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.*;

import java.util.ArrayList;
import java.util.HashMap;
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

        if (amidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL)==0){
            return 0;
        }
        else if (amidstVar.getDistributionType().compareTo(DistType.GAUSSIAN)==0) {

            boolean multinomialParents = false;
            boolean normalParents = false;

            for (Variable v : conditioningVariables) {
                if (v.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                    multinomialParents = true;
                } else if (v.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {
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
        if (variable.getNumberOfStates()<0)
            nstates=2;
        else
            nstates=variable.getNumberOfStates();

        builder.setStateSpace(new MultinomialStateSpace(nstates));
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


/*
    public static void printHuginBNInfo (Domain huginBN) throws ExceptionHugin {


        System.out.println("======================================");
        System.out.println("Hugin Bayesian network ...");
        System.out.println("======================================");
        NodeList huginNodes = huginBN.getNodes();

        System.out.println("Nodes: " + huginNodes.toString());

        System.out.println("Structure:");

        for(int i=0;i<huginNodes.size();i++){

            Node huginChild = (Node)huginNodes.get(i);
            String childName = huginChild.getName();

            NodeList huginParents = huginChild.getParents();
            for (int j=0;j<huginParents.size();j++){
                Node huginParent = (Node)huginParents.get(j);
                String parentName = huginParent.getName();
                System.out.println("  " + parentName + " -> " + childName);
            }
        }

        System.out.println("Distributions:");

        for (int i=0;i<huginNodes.size();i++){
            Node huginNode = (Node)huginNodes.get(i);

            System.out.println(huginNode.getName());

           if (huginNode.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0){
               double[] probabilities  = huginNode.getTable().getData();
               for(int j=0;j<probabilities.length;j++){
                   System.out.print(probabilities[j]+ " ");
               }
           }
           else {

           }

        }


    }*/


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
