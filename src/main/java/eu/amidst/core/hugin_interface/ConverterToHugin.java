package eu.amidst.core.hugin_interface;

import COM.hugin.HAPI.*;
import eu.amidst.core.distribution.*;
import eu.amidst.core.header.Assignment;
import eu.amidst.core.header.DistType;
import eu.amidst.core.header.StaticModelHeader;
import eu.amidst.core.header.Variable;
import eu.amidst.core.modelstructure.BayesianNetwork;
import eu.amidst.core.modelstructure.DAG;

import eu.amidst.core.modelstructure.ParentSet;
import org.omg.DynamicAny.DynSequence;


import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ConverterToHugin {

    private Domain huginNetwork;


    public ConverterToHugin(){
        try {
            this.huginNetwork = new Domain();
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    public void setNodes(List<Variable> amidstVars) {

        try {
             for (Variable amidstVar: amidstVars) {
                 switch (amidstVar.getDistributionType()) {
                     case MULTINOMIAL:
                         new LabelledDCNode(this.huginNetwork);
                     case GAUSSIAN:
                         new ContinuousChanceNode(this.huginNetwork);
                     default:
                         throw new IllegalArgumentException("Error in variable DistributionBuilder. " +
                                 "Unrecognized DistributionType. ");
                 }
             }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    public void setModelStructure (DAG dag) {

        List<Variable> variables = dag.getModelHeader().getVariables();

        try {
            for (Variable amidstChild: variables) {

                int idChild = amidstChild.getVarID();

                for (Variable amidstParent: dag.getParentSet(amidstChild).getParents()) {

                    int idParent = amidstParent.getVarID();
                    Node huginChild = (Node)this.huginNetwork.getNodes().get(idChild);
                    Node huginParent = (Node)this.huginNetwork.getNodes().get(idParent);
                    huginChild.addParent(huginParent);
                }
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }



    public void setDistributions(ConditionalDistribution[] distributions) {

        try {
            for (ConditionalDistribution dist: distributions) {

                Variable amidstVar = dist.getVariable();

                switch (amidstVar.getDistributionType()) {
                    case MULTINOMIAL: //This covers Multinomial and Multinomial_Parents distributions
                        int idVar = amidstVar.getVarID();
                        Node huginVar = (Node)this.huginNetwork.getNodes().get(idVar);
                        //GOOD NEWS: Hugin indexes the multinomial parents assignments as we do (Koller)

                        int numParents = dist.getConditioningVariables().size();

                        Multinomial[] probabilities = ((Multinomial_MultinomialParents)dist).getProbabilities();

                        double[] finalArray = new double[0];

                        int sourcePosition = 0;
                        int finalPosition;

                        for(int i=0;i<numParents;i++){

                            double[] sourceArray = probabilities[i].getProbabilities();
                            finalPosition = finalArray.length + sourceArray.length;
                            System.arraycopy(sourceArray, sourcePosition, finalArray,finalPosition,1);
                            sourcePosition = finalPosition +1;
                        }
                        huginVar.getTable().setData(finalArray);

                    case GAUSSIAN:



//                        //  NORMAL, NORMAL_NORMAL and NORMAL_MULTIMOMIALNORMAL
//                    case GAUSSIAN:
//                        boolean multinomialParents = false;
//                        boolean normalParents = false;
//                        /* The parents of a gaussian variable are either multinomial and/or normal */
//
//
//
//                        for(Variable parent:parents.getParents()){
//                            if (parent.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
//                                multinomialParents = true;
//                            } else if (parent.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {
//                                normalParents = true;
//                            } else {
//                                throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
//                            }
//                        }
//                        if (normalParents && !multinomialParents){
//                            System.out.println("Normal_NormalParents");
//                        }else if ((!normalParents & multinomialParents)|| (parents.getNumberOfParents()==0)){
//                            System.out.println("Normal_MultinomialParents");
//                        } else if (normalParents & multinomialParents) {
//                            System.out.println("Normal_MultinomialNormalParents");
//                        } else {
//                            throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
//                        }
//                    default:
//                        throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
//                }


                    default:
                        throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
                }
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }




    public void setBayesianNetwork(BayesianNetwork bn) {
        this.setNodes(bn.getStaticModelHeader().getVariables());
        this.setModelStructure(bn.getDAG());
        this.setDistributions(bn.getDistributions());
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



    public static void main (String args[])
    {
        System.out.println("HOLA");
    }
}
