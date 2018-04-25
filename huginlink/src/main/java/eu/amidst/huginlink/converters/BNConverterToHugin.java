package eu.amidst.huginlink.converters;

import COM.hugin.HAPI.*;
import eu.amidst.core.distribution.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * The BNConverterToHugin class converts a Bayesian network model from AMIDST to Hugin.
 */
public class BNConverterToHugin {

    /** Represents the Bayesian network model in Hugin format. */
    private Domain huginBN;

    /**
     * Class constructor.
     * @throws ExceptionHugin
     */
    public BNConverterToHugin() throws ExceptionHugin {
        huginBN = new Domain();
    }

    /**
     * Sets the Hugin nodes from the AMIDST variables.
     * @param amidstBN the Bayesian network model in AMIDST format.
     * @throws ExceptionHugin
     */
    private void setNodes(BayesianNetwork amidstBN) throws ExceptionHugin {

        Variables amidstVars = amidstBN.getVariables();
        int size = amidstVars.getNumberOfVars();

        //Hugin always inserts variables at position 0, i.e, for an order A,B,C, it stores C,B,A
        //A reverse order of the variables is needed instead.
        for(int i=1;i<=size;i++){
            Variable amidstVar = amidstVars.getVariableById(size-i);
            if(amidstVar.isMultinomial()){
                LabelledDCNode n = new LabelledDCNode(this.huginBN);
                n.setName(amidstVar.getName());
                n.setNumberOfStates(amidstVar.getNumberOfStates());
                n.setLabel(amidstVar.getName());
                for (int j=0;j<n.getNumberOfStates();j++){
                    String stateName = ((FiniteStateSpace)amidstVar.getStateSpaceType()).getStatesName(j);
                    n.setStateLabel(j, stateName);
                }
            } else if (amidstVar.isNormal()) {
                ContinuousChanceNode c = new ContinuousChanceNode(this.huginBN);
                c.setName(amidstVar.getName());
            } else {
                throw new IllegalArgumentException("Unrecognized DistributionType:" + amidstVar.getDistributionTypeEnum().toString());
            }
        }
    }

    /**
     * Sets the Hugin model structure from the AMIDST DAG.
     * @param amidstBN the Bayesian network model in AMIDST format.
     * @throws ExceptionHugin
     */
    private void setStructure (BayesianNetwork amidstBN) throws ExceptionHugin {

        DAG dag = amidstBN.getDAG();

        for (Variable amidstChild: amidstBN.getVariables()) {
            for (Variable amidstParent: dag.getParentSet(amidstChild)) {
                Node huginChild = this.huginBN.getNodeByName(amidstChild.getName());
                Node huginParent = this.huginBN.getNodeByName(amidstParent.getName());
                huginChild.addParent(huginParent);
            }
        }
    }

    /**
     * Sets the distribution of a multinomial variable in the Hugin model from the corresponding distribution in the
     * AMIDST model.
     * @param dist the AMIDST distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setMultinomial(Multinomial dist) throws ExceptionHugin {

        Variable amidstVar = dist.getVariable();
        Node huginVar = this.huginBN.getNodeByName(amidstVar.getName());
        int nStates = amidstVar.getNumberOfStates();
        double[] finalArray  = new double[nStates];

        double[] sourceArray = dist.getProbabilities();
        System.arraycopy(sourceArray, 0, finalArray, 0, nStates);
        huginVar.getTable().setData(finalArray);
    }

    /**
     * Sets the distribution of a multinomial variable with multinomial parents in the Hugin model from the
     * corresponding distribution in the AMIDST model.
     *
     * @param dist the AMIDST distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setMultinomial_MultinomialParents(Multinomial_MultinomialParents dist) throws ExceptionHugin {

        Variable amidstVar = dist.getVariable();
        Node huginVar = this.huginBN.getNodeByName(amidstVar.getName());
        List<Multinomial> probabilities = dist.getMultinomialDistributions();
        List<Variable> conditioningVariables = dist.getConditioningVariables();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);
        int nStates = amidstVar.getNumberOfStates();
        int sizeArray = numParentAssignments * nStates;
        double[] finalArray  = new double[sizeArray];

        for(int i=0;i<numParentAssignments;i++){
            double[] sourceArray = probabilities.get(i).getProbabilities();
            System.arraycopy(sourceArray, 0, finalArray, i*nStates, nStates);
        }
        huginVar.getTable().setData(finalArray);
    }

    /**
     * Sets the distribution of a normal variable with normal parents in the Hugin model from the corresponding
     * distribution in the AMIDST model. This method is designed for setting this distribution in a given position in
     * the list of distributions indexed by the multinomial parents assignments.
     * @param dist the AMIDST distribution to be converted.
     * @param i the position in which the distribution will be located. Note that <code>i</code> is equal to 0 when the
     *          variable has no multinomial parents.
     * @throws ExceptionHugin
     */
    private void setNormal_NormalParents(ConditionalLinearGaussian dist, int i) throws ExceptionHugin {

        Variable amidstVar = dist.getVariable();
        List<Variable> normalParents = dist.getConditioningVariables();
        int numNormalParents = normalParents.size();

        Node huginVar = this.huginBN.getNodeByName(amidstVar.getName());

        double variance = Math.pow(dist.getSd(), 2);
        ((ContinuousChanceNode)huginVar).setGamma(variance,i);

        double intercept = dist.getIntercept();
        ((ContinuousChanceNode) huginVar).setAlpha(intercept, i);

        double[] coefficientsParents = dist.getCoeffParents();

        for(int j=0;j<numNormalParents;j++) {
            ContinuousChanceNode huginParent =
                    (ContinuousChanceNode)this.huginBN.getNodeByName(normalParents.get(j).getName());
            ((ContinuousChanceNode)huginVar).setBeta(coefficientsParents[j],huginParent,i);
        }
    }

    /**
     * Sets the distribution of a normal variable in the Hugin model from the corresponding distribution in the AMIDST
     * model.
     * @param dist the AMIDST distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setNormal(Normal dist) throws ExceptionHugin {
        this.setNormal(dist,0);
    }

    /**
     * Sets the distribution of a normal variable in the Hugin model from the corresponding distribution in the AMIDST
     * model. This method is designed for setting the distribution in a given position in the list of distributions
     * indexed by the multinomial parents assignments.
     *
     * @param dist the AMIDST distribution to be converted.
     * @param i the position in which the distribution will be located. Note that <code>i</code> is equal to 0 when the
     *          variable has no multinomial parents.
     * @throws ExceptionHugin
     */
    private void setNormal(Normal dist, int i) throws ExceptionHugin {

        Variable amidstVar = dist.getVariable();
        Node huginVar = this.huginBN.getNodeByName(amidstVar.getName());

        double mean =  dist.getMean();
        double sd = dist.getSd();

        ((ContinuousChanceNode)huginVar).setAlpha(mean, i);
        ((ContinuousChanceNode)huginVar).setGamma(Math.pow(sd,2),i);
    }

    /**
     * Sets the distribution of a normal variable with multinomial parents in the Hugin model from the corresponding
     * distribution in the AMIDST model.
     * @param dist the AMIDST distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setNormal_MultinomialParents(Normal_MultinomialParents dist) throws ExceptionHugin {

        List<Variable> conditioningVariables = dist.getConditioningVariables();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

        for(int i=0;i<numParentAssignments;i++) {
            Normal normal =  dist.getNormal(i);
            this.setNormal(normal, i);
        }
    }

    /**
     * Sets the distribution of a normal variable with multinomial and normal parents in the Hugin model from the
     * corresponding distribution in the AMIDST model.
     * @param dist the AMIDST distribution to be converted.
     * @throws ExceptionHugin
     */
    private void setNormal_MultinomialNormalParents(Normal_MultinomialNormalParents dist) throws ExceptionHugin {

        List<Variable> multinomialParents = dist.getMultinomialParents();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);

        for(int i=0;i<numParentAssignments;i++) {
            this.setNormal_NormalParents(dist.getNormal_NormalParentsDistribution(i),i);
        }
    }

    /**
     * Sets the distributions for all the variables in the Hugin model from the distributions in the AMIDST model.
     * For each variable, the distribution type is determined and the corresponding conversion is carried out.
     * @param amidstBN the Bayesian network model in AMIDST format.
     * @throws ExceptionHugin
     */
    private void setDistributions(BayesianNetwork amidstBN) throws ExceptionHugin {

        for (Variable amidstVar : amidstBN.getVariables()) {

            switch (Utils.getConditionalDistributionType(amidstVar, amidstBN)) {
                case 0:
                    this.setMultinomial_MultinomialParents(amidstBN.getConditionalDistribution(amidstVar));
                    break;
                case 1:
                    this.setNormal_NormalParents(amidstBN.getConditionalDistribution(amidstVar), 0);
                    break;
                case 2:
                    this.setNormal_MultinomialParents(amidstBN.getConditionalDistribution(amidstVar));
                    break;
                case 3:
                    this.setNormal_MultinomialNormalParents(amidstBN.getConditionalDistribution(amidstVar));
                    break;
                case 4:
                    this.setMultinomial(amidstBN.getConditionalDistribution(amidstVar));
                    break;
                case 5:
                    this.setNormal(amidstBN.getConditionalDistribution(amidstVar));
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized DistributionType. ");
            }
        }
    }

    /**
     * Converts a Bayesian network from AMIDST to Hugin format.
     * @param amidstBN the AMIDST Bayesian network to be converted.
     * @return the converted Hugin Bayesian network.
     * @throws ExceptionHugin
     */
    public static Domain convertToHugin(BayesianNetwork amidstBN) throws ExceptionHugin {

        BNConverterToHugin converterToHugin  = new BNConverterToHugin();
        converterToHugin.setNodes(amidstBN);
        converterToHugin.setStructure(amidstBN);
        converterToHugin.setDistributions(amidstBN);

        return converterToHugin.huginBN;
    }
}
