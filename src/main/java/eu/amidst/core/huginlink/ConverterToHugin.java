package eu.amidst.core.huginlink;

import COM.hugin.HAPI.*;

import COM.hugin.HAPI.Node;
import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;
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
            System.out.print("Exception caught: " + e.getMessage());
        }
    }

    public Domain getHuginNetwork(){
        return this.huginNetwork;
    }

    public void setNodes(List<Variable> amidstVars) {

        int size = amidstVars.size();

        try {
            //Hugin always inserts variables in position 0, i.e, for an order A,B,C, it stores C,B,A !!!
            //A reverse order of the variables is used instead.
            for(int i=1;i<=size;i++){
                Variable amidstVar = amidstVars.get(size-i);
                if (amidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                    LabelledDCNode n = new LabelledDCNode(this.huginNetwork);
                    n.setName(amidstVar.getName());
                    n.setNumberOfStates(amidstVar.getNumberOfStates());

                    for (int j=0;j<n.getNumberOfStates();j++){
                        n.setStateLabel(j,amidstVar.getName()+j);
                    }
                } else if (amidstVar.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {
                    ContinuousChanceNode c = new ContinuousChanceNode(this.huginNetwork);
                    c.setName(amidstVar.getName());
                } else {
                    throw new IllegalArgumentException("Unrecognized DistributionType.");
                }
            }
            System.out.println();
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    public void setStructure (DAG dag) {

        List<Variable> variables = dag.getStaticVariables().getListOfVariables();

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
    public void setMultinomial_MultinomialParents(Multinomial_MultinomialParents dist) {


        try {
            Variable amidstVar = dist.getVariable();
            Node huginVar = this.huginNetwork.getNodeByName(amidstVar.getName());
            Multinomial[] probabilities = dist.getProbabilities();
            List<Variable> conditioningVariables = dist.getConditioningVariables();
            int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);
            int nStates = amidstVar.getNumberOfStates();
            int sizeArray = numParentAssignments * nStates;
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

    public void setNormal_NormalParents(Normal_NormalParents dist, int assign_i) {

        try {
            Variable amidstVar = dist.getVariable();
            List<Variable> normalParents = dist.getConditioningVariables();
            int numNormalParents = normalParents.size();

            Node huginVar = this.huginNetwork.getNodeByName(amidstVar.getName());

            double variance = Math.pow(dist.getSd(), 2);
            ((ContinuousChanceNode)huginVar).setGamma(variance,assign_i);

            double intercept = dist.getIntercept();
            ((ContinuousChanceNode) huginVar).setAlpha(intercept, assign_i);

            double[] coeffParents = dist.getCoeffParents();

            for(int i=0;i<numNormalParents;i++) {
                ContinuousChanceNode huginParent =
                        (ContinuousChanceNode)this.huginNetwork.getNodeByName(normalParents.get(i).getName());
                ((ContinuousChanceNode)huginVar).setBeta(coeffParents[i],huginParent,assign_i);
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

    public void setNormal_MultinomialParents(Normal_MultinomialParents dist) {

        List<Variable> conditioningVariables = dist.getConditioningVariables();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

        for(int i=0;i<numParentAssignments;i++) {
            Normal normal =  dist.getNormal(i);
            this.setNormal(normal, i );
        }
    }


    public void setNormal_MultinomialNormalParents(Normal_MultinomialNormalParents dist){

        List<Variable> multinomialParents = dist.getMultinomialParents();

        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);

        for(int i=0;i<numParentAssignments;i++) {
            this.setNormal_NormalParents(dist.getNormal_NormalParentsDistribution(i),i);
        }
    }

    public void setDistributions(BayesianNetwork bn) {

        List<Variable> amidstVars = bn.getVariables();

        for(Variable amidstVar:amidstVars) {


            List<Variable> conditioningVariables = bn.getDistribution(amidstVar).getConditioningVariables();

            if (amidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL)==0){
                this.setMultinomial_MultinomialParents(bn.getDistribution(amidstVar));
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
                if (normalParents && !multinomialParents){
                    this.setNormal_NormalParents(bn.getDistribution(amidstVar),0);
                }
                else if ((!normalParents & multinomialParents) || (conditioningVariables.size() == 0)) {
                    this.setNormal_MultinomialParents(bn.getDistribution(amidstVar));
                }
                else if (normalParents & multinomialParents) {
                    this.setNormal_MultinomialNormalParents(bn.getDistribution(amidstVar));
                }
                else {
                    throw new IllegalArgumentException("Unrecognized DistributionType. ");
                }
            }
            else {
                throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
    }

    public void convertToHuginBN(BayesianNetwork bn) {

        this.setNodes(bn.getDAG().getStaticVariables().getListOfVariables());
        this.setStructure(bn.getDAG());
        this.setDistributions(bn);
    }
}
