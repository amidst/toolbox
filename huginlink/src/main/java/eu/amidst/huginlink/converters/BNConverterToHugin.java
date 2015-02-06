package eu.amidst.huginlink.converters;

import COM.hugin.HAPI.*;
import eu.amidst.core.distribution.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.FiniteStateSpace;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.List;

public class BNConverterToHugin {

    private Domain huginBN;

    public BNConverterToHugin() throws ExceptionHugin {
        huginBN = new Domain();
    }

    private void setNodes(BayesianNetwork amidstBN) throws ExceptionHugin {

        StaticVariables amidstVars = amidstBN.getStaticVariables();
        int size = amidstVars.getNumberOfVars();

        //Hugin always inserts variables in position 0, i.e, for an order A,B,C, it stores C,B,A !!!
        //A reverse order of the variables is used instead.
        for(int i=1;i<=size;i++){
            Variable amidstVar = amidstVars.getVariableById(size-i);
            if(amidstVar.isMultinomial()){
                LabelledDCNode n = new LabelledDCNode(this.huginBN);
                n.setName(amidstVar.getName());
                n.setNumberOfStates(amidstVar.getNumberOfStates());
                n.setLabel(amidstVar.getName());
                for (int j=0;j<n.getNumberOfStates();j++){
                    String stateName = ((FiniteStateSpace)amidstVar.getStateSpace()).getStatesName(j);
                    n.setStateLabel(j, stateName);
                }
            } else if (amidstVar.isGaussian()) {
                ContinuousChanceNode c = new ContinuousChanceNode(this.huginBN);
                c.setName(amidstVar.getName());
            } else {
                throw new IllegalArgumentException("Unrecognized DistributionType:" + amidstVar.getDistributionType().toString());
            }
        }
    }

    private void setStructure (BayesianNetwork amidstBN) throws ExceptionHugin {

        DAG dag = amidstBN.getDAG();

        for (Variable amidstChild: amidstBN.getStaticVariables()) {
            for (Variable amidstParent: dag.getParentSet(amidstChild)) {
                Node huginChild = this.huginBN.getNodeByName(amidstChild.getName());
                Node huginParent = this.huginBN.getNodeByName(amidstParent.getName());
                huginChild.addParent(huginParent);
            }
        }
    }

    private void setMultinomial_MultinomialParents(Multinomial_MultinomialParents dist) throws ExceptionHugin {

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

    private void setNormal_NormalParents(Normal_NormalParents dist, int assign) throws ExceptionHugin {

        Variable amidstVar = dist.getVariable();
        List<Variable> normalParents = dist.getConditioningVariables();
        int numNormalParents = normalParents.size();

        Node huginVar = this.huginBN.getNodeByName(amidstVar.getName());

        double variance = Math.pow(dist.getSd(), 2);
        ((ContinuousChanceNode)huginVar).setGamma(variance,assign);

        double intercept = dist.getIntercept();
        ((ContinuousChanceNode) huginVar).setAlpha(intercept, assign);

        double[] coeffParents = dist.getCoeffParents();

        for(int i=0;i<numNormalParents;i++) {
            ContinuousChanceNode huginParent =
                    (ContinuousChanceNode)this.huginBN.getNodeByName(normalParents.get(i).getName());
            ((ContinuousChanceNode)huginVar).setBeta(coeffParents[i],huginParent,assign);
        }
    }

    private void setNormal(Normal dist, int i) throws ExceptionHugin {

        Variable amidstVar = dist.getVariable();
        Node huginVar = this.huginBN.getNodeByName(amidstVar.getName());

        double mean =  dist.getMean();
        double sd = dist.getSd();

        ((ContinuousChanceNode)huginVar).setAlpha(mean, i);
        ((ContinuousChanceNode)huginVar).setGamma(Math.pow(sd,2),i);
    }

    private void setNormal_MultinomialParents(Normal_MultinomialParents dist) throws ExceptionHugin {

        List<Variable> conditioningVariables = dist.getConditioningVariables();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

        for(int i=0;i<numParentAssignments;i++) {
            Normal normal =  dist.getNormal(i);
            this.setNormal(normal, i);
        }
    }

    private void setNormal_MultinomialNormalParents(Normal_MultinomialNormalParents dist) throws ExceptionHugin {

        List<Variable> multinomialParents = dist.getMultinomialParents();

        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);

        for(int i=0;i<numParentAssignments;i++) {
            this.setNormal_NormalParents(dist.getNormal_NormalParentsDistribution(i),i);
        }
    }

    private void setDistributions(BayesianNetwork amidstBN) throws ExceptionHugin {

        for (Variable amidstVar : amidstBN.getStaticVariables()) {

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
                    throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
    }

    public static Domain convertToHugin(BayesianNetwork amidstBN) throws ExceptionHugin {

        BNConverterToHugin converterToHugin  = new BNConverterToHugin();
        converterToHugin.setNodes(amidstBN);
        converterToHugin.setStructure(amidstBN);
        converterToHugin.setDistributions(amidstBN);

        return converterToHugin.huginBN;
    }
}
