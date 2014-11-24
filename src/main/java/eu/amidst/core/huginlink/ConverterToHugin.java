package eu.amidst.core.huginlink;

import COM.hugin.HAPI.*;

import COM.hugin.HAPI.Node;
import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.List;

public class ConverterToHugin {

    private Domain huginBN;
    private BayesianNetwork amidstBN;

    public ConverterToHugin(BayesianNetwork amidstBN){
        try {
            this.huginBN = new Domain();
        }
        catch (ExceptionHugin e) {
            System.out.print("Exception caught: " + e.getMessage());
        }
        this.amidstBN = amidstBN;
    }

    public Domain getHuginNetwork(){
        return this.huginBN;
    }

<<<<<<< HEAD
    public void setNodes() {
=======
    private void setNodes(List<Variable> amidstVars) {
>>>>>>> 854b35daa4f9ad232e61133d34ff4f925975ddb0

        List<Variable> amidstVars = amidstBN.getListOfVariables();
        int size = amidstVars.size();

        try {
            //Hugin always inserts variables in position 0, i.e, for an order A,B,C, it stores C,B,A !!!
            //A reverse order of the variables is used instead.
            for(int i=1;i<=size;i++){
                Variable amidstVar = amidstVars.get(size-i);
                if (amidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                    LabelledDCNode n = new LabelledDCNode(this.huginBN);
                    n.setName(amidstVar.getName());
                    n.setNumberOfStates(amidstVar.getNumberOfStates());

                    for (int j=0;j<n.getNumberOfStates();j++){
                        n.setStateLabel(j,amidstVar.getName()+j);
                    }
                } else if (amidstVar.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {
                    ContinuousChanceNode c = new ContinuousChanceNode(this.huginBN);
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

<<<<<<< HEAD
    public void setStructure () {
=======
    private void setStructure (DAG dag) {
>>>>>>> 854b35daa4f9ad232e61133d34ff4f925975ddb0

        DAG dag = amidstBN.getDAG();

        List<Variable> variables = amidstBN.getListOfVariables();

        try {
            for (Variable amidstChild: variables) {
                for (Variable amidstParent: dag.getParentSet(amidstChild).getParents()) {
                    Node huginChild = this.huginBN.getNodeByName(amidstChild.getName());
                    Node huginParent = this.huginBN.getNodeByName(amidstParent.getName());
                    huginChild.addParent(huginParent);
                }
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

<<<<<<< HEAD
    public void setMultinomial_MultinomialParents(Multinomial_MultinomialParents dist) {
=======
    //GOOD NEWS: Hugin indexes the multinomial parents assignments as we do (Koller)
    private void setMultinomial_MultinomialParents(Multinomial_MultinomialParents dist) {
>>>>>>> 854b35daa4f9ad232e61133d34ff4f925975ddb0


        try {
            Variable amidstVar = dist.getVariable();
            Node huginVar = this.huginBN.getNodeByName(amidstVar.getName());
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

<<<<<<< HEAD
    public void setNormal_NormalParents(Normal_NormalParents dist, int assign_i) throws ExceptionHugin {
=======
    private void setNormal_NormalParents(Normal_NormalParents dist, int assign_i) {
>>>>>>> 854b35daa4f9ad232e61133d34ff4f925975ddb0

        Variable amidstVar = dist.getVariable();
        List<Variable> normalParents = dist.getConditioningVariables();
        int numNormalParents = normalParents.size();

        Node huginVar = this.huginBN.getNodeByName(amidstVar.getName());

        double variance = Math.pow(dist.getSd(), 2);
        ((ContinuousChanceNode)huginVar).setGamma(variance,assign_i);

        double intercept = dist.getIntercept();
        ((ContinuousChanceNode) huginVar).setAlpha(intercept, assign_i);

        double[] coeffParents = dist.getCoeffParents();

        for(int i=0;i<numNormalParents;i++) {
            ContinuousChanceNode huginParent =
                    (ContinuousChanceNode)this.huginBN.getNodeByName(normalParents.get(i).getName());
            ((ContinuousChanceNode)huginVar).setBeta(coeffParents[i],huginParent,assign_i);
        }
    }

<<<<<<< HEAD
    public void setNormal(Normal dist, int i) throws ExceptionHugin {
=======
    private void setNormal(Normal dist, int i) {
>>>>>>> 854b35daa4f9ad232e61133d34ff4f925975ddb0

            Variable amidstVar = dist.getVariable();
            Node huginVar = this.huginBN.getNodeByName(amidstVar.getName());

            double mean =  dist.getMean();
            double sd = dist.getSd();

            ((ContinuousChanceNode)huginVar).setAlpha(mean, i);
            ((ContinuousChanceNode)huginVar).setGamma(Math.pow(sd,2),i);
    }

<<<<<<< HEAD
    public void setNormal_MultinomialParents(Normal_MultinomialParents dist) throws ExceptionHugin {
=======
    private void setNormal_MultinomialParents(Normal_MultinomialParents dist) {
>>>>>>> 854b35daa4f9ad232e61133d34ff4f925975ddb0

        List<Variable> conditioningVariables = dist.getConditioningVariables();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

        for(int i=0;i<numParentAssignments;i++) {
            Normal normal =  dist.getNormal(i);
            this.setNormal(normal, i );
        }
    }

<<<<<<< HEAD
    public void setNormal_MultinomialNormalParents(Normal_MultinomialNormalParents dist) throws ExceptionHugin {
=======

    private void setNormal_MultinomialNormalParents(Normal_MultinomialNormalParents dist){
>>>>>>> 854b35daa4f9ad232e61133d34ff4f925975ddb0

        List<Variable> multinomialParents = dist.getMultinomialParents();

        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);

        for(int i=0;i<numParentAssignments;i++) {
            this.setNormal_NormalParents(dist.getNormal_NormalParentsDistribution(i),i);
        }
    }

<<<<<<< HEAD
    public void setDistributions() throws ExceptionHugin {

        List<Variable> amidstVars = amidstBN.getListOfVariables();

        for (Variable amidstVar : amidstVars) {

            switch (Utils.getConditionalDistributionType(amidstVar, amidstBN)) {
                case 0:
                    this.setMultinomial_MultinomialParents(amidstBN.getDistribution(amidstVar));
                    break;
                case 1:
                    this.setNormal_NormalParents(amidstBN.getDistribution(amidstVar), 0);
                    break;
                case 2:
                    this.setNormal_MultinomialParents(amidstBN.getDistribution(amidstVar));
                    break;
                case 3:
                    this.setNormal_MultinomialNormalParents(amidstBN.getDistribution(amidstVar));
                    break;
                default:
=======
    private void setDistributions(BayesianNetwork bn) {

        List<Variable> amidstVars = bn.getListOfVariables();

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
>>>>>>> 854b35daa4f9ad232e61133d34ff4f925975ddb0
                    throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
    }

    public void convertToHuginBN() throws ExceptionHugin {


        this.setNodes();
        this.setStructure();
        this.setDistributions();



    }
}
