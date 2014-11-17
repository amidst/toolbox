package eu.amidst.core.hugin_interface;

import COM.hugin.HAPI.*;

import eu.amidst.core.distribution.*;
import eu.amidst.core.header.DistType;
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


    //GOOD NEWS: Hugin indexes the multinomial parents assignments as we do (Koller)
    public void setMultinomial_MultinomialParents(ConditionalDistribution dist) {

        try {
            Variable amidstVar = dist.getVariable();
            int idVar = amidstVar.getVarID();
            Node huginVar = (Node)this.huginNetwork.getNodes().get(idVar);

            Multinomial[] probabilities = ((Multinomial_MultinomialParents)dist).getProbabilities();
            List<Variable> conditioningVariables = dist.getConditioningVariables();
            int numParents = conditioningVariables.size();

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

        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }

    }

    //GOOD NEWS: Hugin indexes the multinomial parents assignments as we do (Koller)
    public void setNormal_NormalParents(ConditionalDistribution dist, int i) {

        try {
            Variable amidstVar = dist.getVariable();
            int idVar = amidstVar.getVarID();
            Node huginVar = (Node)this.huginNetwork.getNodes().get(idVar);
            List<Variable> conditioningVariables = dist.getConditioningVariables();
            int numParents = conditioningVariables.size();

            double variance = Math.pow(((Normal_NormalParents) amidstVar).getSd(), 2);
            ((ContinuousChanceNode)huginVar).setGamma(variance,i);

            double intercept = ((Normal_NormalParents)amidstVar).getIntercept();
            ((ContinuousChanceNode)huginVar).setAlpha(intercept,i);


            double[] coeffParents = ((Normal_NormalParents)amidstVar).getCoeffParents();
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
            int idVar = amidstVar.getVarID();
            Node huginVar = (Node)this.huginNetwork.getNodes().get(idVar);

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

        try {
            Variable amidstVar = dist.getVariable();
            int idVar = amidstVar.getVarID();
            Node huginVar = (Node)this.huginNetwork.getNodes().get(idVar);
            List<Variable> conditioningVariables = dist.getConditioningVariables();

            int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

            for(int i=0;i<numParentAssignments;i++) {
                Normal normal =  ((Normal_MultinomialParents)dist).getNormal(i);
                this.setNormal(normal, i );
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
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

        for (ConditionalDistribution dist: distributions) {

            Variable amidstVar = dist.getVariable();
            List<Variable> conditioningVariables = dist.getConditioningVariables();

            switch (amidstVar.getDistributionType()) {
                case MULTINOMIAL:
                   this.setMultinomial_MultinomialParents(dist);
                case GAUSSIAN:
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

                default:
                    throw new IllegalArgumentException("Error in variable DistributionBuilder. Unrecognized DistributionType. ");
            }
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
