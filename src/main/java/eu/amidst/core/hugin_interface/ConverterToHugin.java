package eu.amidst.core.hugin_interface;

import COM.hugin.HAPI.*;

//import COM.hugin.HAPI.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.distribution.*;
import eu.amidst.core.header.DistType;
import eu.amidst.core.header.StateSpaceType;
import eu.amidst.core.header.StaticModelHeader;
import eu.amidst.core.header.Variable;
import eu.amidst.core.modelstructure.BayesianNetwork;
import eu.amidst.core.modelstructure.DAG;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
                System.out.print(amidstVar.getName()+ " ");
                if (amidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                    LabelledDCNode n = new LabelledDCNode(this.huginNetwork);
                    n.setName(amidstVar.getName());
                    n.setNumberOfStates(amidstVar.getNumberOfStates());

                    for (int i=0;i<n.getNumberOfStates();i++){
                        n.setStateLabel(i,amidstVar.getName()+i);
                    }



                } else if (amidstVar.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {
                    ContinuousChanceNode c = new ContinuousChanceNode(this.huginNetwork);
                    c.setName(amidstVar.getName());

                } else {
                    throw new IllegalArgumentException("Error in class ConverterToHugin. " +
                            "Unrecognized DistributionType. ");
                }
            }
            System.out.println();
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    public void setModelStructure (DAG dag) {

        List<Variable> variables = dag.getModelHeader().getVariables();

        try {
            for (Variable amidstChild: variables) {
                for (Variable amidstParent: dag.getParentSet(amidstChild).getParents()) {
                    Node huginChild = this.huginNetwork.getNodeByName(amidstChild.getName());
                    Node huginParent = this.huginNetwork.getNodeByName(amidstParent.getName());
                    huginChild.addParent(huginParent);
                    System.out.println(huginParent.getName() + " --> " + huginChild.getName());
                }
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }


    //GOOD NEWS: Hugin indexes the multinomial parents assignments as we do (Koller)
    public void setMultinomial_MultinomialParents(ConditionalDistribution dist) {

        System.out.print(" (Multinomial_MultinomialParents)\n");
        try {
            Variable amidstVar = dist.getVariable();

            Node huginVar = this.huginNetwork.getNodeByName(amidstVar.getName());

            Multinomial[] probabilities = ((Multinomial_MultinomialParents)dist).getProbabilities();

            List<Variable> conditioningVariables = dist.getConditioningVariables();

            int numParents = conditioningVariables.size();

            //System.out.println("    Number of parents:" + numParents);


            int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);
            //System.out.println("Number of parent assignment:" + numParentAssignments);

            int nStates = amidstVar.getNumberOfStates();
            //System.out.println("Number of states:" + nStates);

            int sizeArray = numParentAssignments * nStates;

            //System.out.println("sizeArray:" + nStates);

            double[] finalArray  = new double[sizeArray];

            for(int i=0;i<numParentAssignments;i++){
                double[] sourceArray = probabilities[i].getProbabilities();
                System.arraycopy(sourceArray, 0, finalArray, i*nStates, nStates);
            }
            huginVar.getTable().setData(finalArray);
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }

    }

    //GOOD NEWS: Hugin indexes the multinomial parents assignments as we do (Koller)
    public void setNormal_NormalParents(ConditionalDistribution dist, int assign_i) {

        System.out.print(" (Normal_NormalParents)\n");
        try {
            Variable amidstVar = dist.getVariable();
            Node huginVar = this.huginNetwork.getNodeByName(amidstVar.getName());
            List<Variable> conditioningVariables = dist.getConditioningVariables();
            int numParents = conditioningVariables.size();

            double variance = Math.pow(((Normal_NormalParents) dist).getSd(), 2);
            ((ContinuousChanceNode)huginVar).setGamma(variance,assign_i);

            double intercept = ((Normal_NormalParents) dist).getIntercept();
            ((ContinuousChanceNode) huginVar).setAlpha(intercept, assign_i);

            double[] coeffParents = ((Normal_NormalParents)dist).getCoeffParents();

            for(int j=0;j<numParents;j++) {
                ContinuousChanceNode parent = (ContinuousChanceNode)huginVar.getParents().get(j);
                ((ContinuousChanceNode)huginVar).setBeta(coeffParents[j],parent,assign_i);
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }

    }

    public void setNormal(Normal dist, int i) {

        try {
            Variable amidstVar = dist.getVariable();
            Node huginVar = this.huginNetwork.getNodeByName(amidstVar.getName());

            double mean =  dist.getMean();
            double sd = dist.getSd();

            ((ContinuousChanceNode)huginVar).setAlpha(mean, i);
            ((ContinuousChanceNode)huginVar).setGamma(Math.pow(sd,2),i);
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    public void setNormal_MultinomialParents(ConditionalDistribution dist) {

        System.out.print(" (Normal_MultinomialParents)\n");

        List<Variable> conditioningVariables = dist.getConditioningVariables();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

        for(int i=0;i<numParentAssignments;i++) {
            Normal normal =  ((Normal_MultinomialParents)dist).getNormal(i);
            this.setNormal(normal, i );
        }
    }


    public void setNormal_MultinomialNormalParents(ConditionalDistribution dist){

        System.out.print(" (Normal_MultinomialNormalParents)\n");
        Variable amidstVar = dist.getVariable();

        List<Variable> multinomialParents = ((Normal_MultinomialNormalParents)dist).getMultinomialParents();

        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);

        for(int i=0;i<numParentAssignments;i++) {
            ConditionalDistribution normal_normalParents = ((Normal_MultinomialNormalParents)dist).getNormal_NormalParentsDistribution(i);
            this.setNormal_NormalParents(normal_normalParents,i);
        }
    }

    public void setDistributions(ConditionalDistribution[] distributions) {

        for (ConditionalDistribution dist: distributions) {

            Variable amidstVar = dist.getVariable();
            List<Variable> conditioningVariables = dist.getConditioningVariables();

            System.out.print("Variable:"+ amidstVar.getName());

            if (amidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL)==0){
                this.setMultinomial_MultinomialParents(dist);
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
                        throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
                    }
                }
                if (normalParents && !multinomialParents){
                    this.setNormal_NormalParents(dist,0);
                }
                else if ((!normalParents & multinomialParents) || (conditioningVariables.size() == 0)) {
                    this.setNormal_MultinomialParents(dist);
                }
                else if (normalParents & multinomialParents) {
                    this.setNormal_MultinomialNormalParents(dist);
                }
                else {
                    throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
                }
            }
            else {
                throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
            }
        }
    }

    public void setBayesianNetwork(BayesianNetwork bn) {
        System.out.println("===================== SET NODES ============================");
        this.setNodes(bn.getDAG().getModelHeader().getVariables());
        System.out.println("================== SET STRUCTURE ===========================");
        this.setModelStructure(bn.getDAG());
        System.out.println("================= SET DISTRIBUTIONS ========================");
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




    public static void main (String args[]) throws ExceptionHugin {

        //**************************************** Synthetic data ******************************************************

        WekaDataFileReader fileReader = new WekaDataFileReader(
                new String("/Users/afa/Dropbox/AMIDST-AFA/core/datasets/syntheticData.arff"));

        StaticModelHeader modelHeader = new StaticModelHeader(fileReader.getAttributes());

        //***************************************** Network structure **************************************************

        DAG dag = new DAG(modelHeader);
        List<Variable> variables =  dag.getModelHeader().getVariables();

        Variable A,B,C,D,E,G,H,I;
        A = variables.get(0); B = variables.get(1); C = variables.get(2); D = variables.get(3);
        E = variables.get(4); G = variables.get(5); H = variables.get(6); I = variables.get(7);


        dag.getParentSet(E).addParent(A);
        dag.getParentSet(E).addParent(B);

        dag.getParentSet(H).addParent(A);
        dag.getParentSet(H).addParent(B);

        dag.getParentSet(I).addParent(A);
        dag.getParentSet(I).addParent(B);
        dag.getParentSet(I).addParent(C);
        dag.getParentSet(I).addParent(D);

        dag.getParentSet(G).addParent(C);
        dag.getParentSet(G).addParent(D);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        //****************************************** Distributions *****************************************************

        bn.initializeDistributions();

        //**************************************************************************************************************

        ConverterToHugin converter = new ConverterToHugin();
        converter.setBayesianNetwork(bn);
        converter.huginNetwork.saveAsNet(new String("/Users/afa/Dropbox/AMIDST-AFA/core/networks/huginNetworkFromAMIDST.net"));
    }
}
