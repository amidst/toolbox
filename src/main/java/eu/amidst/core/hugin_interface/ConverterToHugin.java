package eu.amidst.core.hugin_interface;

import COM.hugin.HAPI.*;

//import COM.hugin.HAPI.Attribute;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.distribution.*;
import eu.amidst.core.header.DistType;
import eu.amidst.core.header.StaticModelHeader;
import eu.amidst.core.header.Variable;
import eu.amidst.core.modelstructure.BayesianNetwork;
import eu.amidst.core.modelstructure.DAG;
import eu.amidst.core.utils.MultinomialIndex;

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


        System.out.println("Converting the set of nodes ...");
        try {
             for (Variable amidstVar: amidstVars) {
                 if (amidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                      new LabelledDCNode(this.huginNetwork).setName(amidstVar.getName());
                 } else if (amidstVar.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {
                     new ContinuousChanceNode(this.huginNetwork).setName(amidstVar.getName());
                 } else {
                     throw new IllegalArgumentException("Error in class ConverterToHugin. " +
                             "Unrecognized DistributionType. ");
                 }
             }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    public void setModelStructure (DAG dag) {

        System.out.println("Converting the model structure ...");

        List<Variable> variables = dag.getModelHeader().getVariables();

        try {
            for (Variable amidstChild: variables) {
                for (Variable amidstParent: dag.getParentSet(amidstChild).getParents()) {
                    Node huginChild = this.huginNetwork.getNodeByName(amidstChild.getName());
                    Node huginParent = this.huginNetwork.getNodeByName(amidstParent.getName());
                    huginChild.addParent(huginParent);
                }
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }


    //GOOD NEWS: Hugin indexes the multinomial parents assignments as we do (Koller)
    public void setMultinomial_MultinomialParents(ConditionalDistribution dist) {

        System.out.println("Multinomial_MultinomialParents:\n");
        try {
            Variable amidstVar = dist.getVariable();
            System.out.println("-------->" + amidstVar.getName());
            Node huginVar = this.huginNetwork.getNodeByName(amidstVar.getName());

            Multinomial[] probabilities = ((Multinomial_MultinomialParents)dist).getProbabilities();
            System.out.println("SIZE: "+ probabilities.length);

            System.out.println("---->" + probabilities[0]);

            List<Variable> conditioningVariables = dist.getConditioningVariables();
            int numParents = conditioningVariables.size();

            double[] finalArray = new double[0];
            int sourcePosition = 0;
            int finalPosition;

            for(int i=0;i<numParents;i++){
                System.out.println(probabilities[i].getProbabilities());
                double[] sourceArray = probabilities[i].getProbabilities();
                finalPosition = finalArray.length + sourceArray.length;
                System.arraycopy(sourceArray, sourcePosition, finalArray,finalPosition,1);
                sourcePosition = finalPosition +1;
            }


            huginVar.getTable().setData(finalArray);

        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }

    }

    //GOOD NEWS: Hugin indexes the multinomial parents assignments as we do (Koller)
    public void setNormal_NormalParents(ConditionalDistribution dist, int i) {

        try {
            Variable amidstVar = dist.getVariable();
            Node huginVar = this.huginNetwork.getNodeByName(amidstVar.getName());
            List<Variable> conditioningVariables = dist.getConditioningVariables();
            int numParents = conditioningVariables.size();

            double variance = Math.pow(((Normal_NormalParents) dist).getSd(), 2);
            ((ContinuousChanceNode)huginVar).setGamma(variance,i);

            double intercept = ((Normal_NormalParents) dist).getIntercept();
                    ((ContinuousChanceNode) huginVar).setAlpha(intercept, i);


            double[] coeffParents = ((Normal_NormalParents)dist).getCoeffParents();
            for(int j=0;j<numParents;j++) {
                Node p = (Node)huginVar.getParents().get(j);
                ((ContinuousChanceNode)huginVar).setBeta(coeffParents[j],(ContinuousChanceNode)p,j);
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

    //GOOD NEWS: Hugin indexes the multinomial parents assignments as we do (Koller)

    public void setNormal_MultinomialParents(ConditionalDistribution dist) {

        List<Variable> conditioningVariables = dist.getConditioningVariables();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

        for(int i=0;i<numParentAssignments;i++) {
            Normal normal =  ((Normal_MultinomialParents)dist).getNormal(i);
            this.setNormal(normal, i );
        }

    }


    public void setNormal_MultinomialNormalParents(ConditionalDistribution dist){

        try {
            Variable amidstVar = dist.getVariable();
            int idVar = amidstVar.getVarID();
            Node huginVar = (Node)this.huginNetwork.getNodes().get(idVar);

            List<Variable> multinomialParents = ((Normal_MultinomialNormalParents)dist).getMultinomialParents();

            int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);

            for(int i=0;i<numParentAssignments;i++) {
                ConditionalDistribution normal_normalParents = ((Normal_MultinomialNormalParents)dist).getNormal_NormalParentsDistribution(i);
                this.setNormal_NormalParents(normal_normalParents,i);
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
  }

   public void setDistributions(ConditionalDistribution[] distributions) {

        System.out.println("Converting the distributions ...");

       for (ConditionalDistribution dist: distributions) {

            Variable amidstVar = dist.getVariable();
            List<Variable> conditioningVariables = dist.getConditioningVariables();

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
                    System.out.println("Normal_NormalParents:\n");
                    this.setNormal_NormalParents(dist,0);
                }
                else if ((!normalParents & multinomialParents) || (conditioningVariables.size() == 0)) {
                    System.out.println("Normal_MultinomialParents:\n");
                    this.setNormal_MultinomialParents(dist);
                }
                else if (normalParents & multinomialParents) {
                    System.out.println("Normal_MultinomialNormalParents:\n");
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
        this.setNodes(bn.getDAG().getModelHeader().getVariables());
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




    public static void main (String args[]) throws ExceptionHugin {

        //**************************************** Synthetic data ******************************************************

        WekaDataFileReader fileReader = new WekaDataFileReader(
                new String("/Users/afa/Dropbox/AMIDST-AFA/core/datasets/syntheticData.arff"));





        StaticModelHeader modelHeader = new StaticModelHeader(fileReader.getAttributes());

        Attribute at = fileReader.getAttributes().getList().iterator().next();
        System.out.println("STATES" + at.getNumberOfStates());
         at = fileReader.getAttributes().getList().iterator().next();
        System.out.println("STATES" + at.getNumberOfStates());
         at = fileReader.getAttributes().getList().iterator().next();
        System.out.println("STATES" + at.getNumberOfStates());
         at = fileReader.getAttributes().getList().iterator().next();
        System.out.println("STATES" + at.getNumberOfStates());
         at = fileReader.getAttributes().getList().iterator().next();
        System.out.println("STATES" + at.getNumberOfStates());
         at = fileReader.getAttributes().getList().iterator().next();
        System.out.println("STATES" + at.getNumberOfStates());
        //***************************************** Network structure **************************************************

        DAG dag = new DAG(modelHeader);
        List<Variable> variables =  dag.getModelHeader().getVariables();

        Variable A,B,C,D,E,G,H,I;
        A = variables.get(0); H = variables.get(1); B = variables.get(2); G = variables.get(3);
        D = variables.get(4); C = variables.get(5); I = variables.get(6); E = variables.get(7);


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
