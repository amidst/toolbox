package eu.amidst.huginlink.inference;

import COM.hugin.HAPI.*;
import eu.amidst.corestatic.distribution.UnivariateDistribution;
import eu.amidst.corestatic.inference.InferenceAlgorithm;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.utils.BayesianNetworkGenerator;
import eu.amidst.corestatic.variables.Assignment;
import eu.amidst.corestatic.variables.HashMapAssignment;
import eu.amidst.corestatic.variables.Variable;
import eu.amidst.corestatic.distribution.Multinomial;
import eu.amidst.corestatic.distribution.Normal;
import eu.amidst.huginlink.converters.BNConverterToHugin;
import eu.amidst.huginlink.io.BNWriterToHugin;

/**
 * This class provides an interface to perform Bayesian network inference using the Hugin inference engine.
 *
 * TODO: Implment method getLogProbabilityOfEvidence
 * @author Antonio Fernández
 * @version 1.0
 * @since 9/2/15
 */
public class HuginInference implements InferenceAlgorithm {

    /**
     * The Bayesian network model in AMIDST format.
     */
    BayesianNetwork amidstBN;

    /**
     * The Bayesian network model in Hugin format.
     */
    Domain huginBN;

    /**
     * Sets an evidence to a Hugin variable.
     *
     * @param n the AMIDST variable to be evidenced.
     * @param value the evidenced value.
     * @throws ExceptionHugin
     */
    private void setVarEvidence(Variable n, double value) throws ExceptionHugin {
        if (n.isMultinomial()){
            ((DiscreteNode)huginBN.getNodeByName(n.getName())).selectState((long)value);
        }
        else if (n.isNormal()) {
            ((ContinuousChanceNode)huginBN.getNodeByName(n.getName())).enterValue(value);
        }
        else {
            throw new IllegalArgumentException("Variable type not allowed.");
        }
    }

    /**
     * Prints the belief of a Hugin node.
     *
     * @param node the node whose belief is printed.
     * @throws ExceptionHugin
     */
    private void printBelief(Node node) throws ExceptionHugin {
        if (node instanceof DiscreteNode) {
            DiscreteNode dNode = (DiscreteNode) node;
            int n = (int) dNode.getNumberOfStates();
            for (int i=0;i<n;i++) {
                System.out.print ("  -> " + dNode.getStateLabel(i)+ " " + dNode.getBelief(i));
                System.out.println();
            }
        } else {
            ContinuousChanceNode ccNode = (ContinuousChanceNode) node;
            System.out.println ("  - Mean : " + ccNode.getMean());
            System.out.println ("  - SD   : " + Math.sqrt (ccNode.getVariance()));
        }
    }

    /**
     * Prints the beliefs of the Hugin network.
     *
     * @throws ExceptionHugin
     */
    private void printBeliefs () throws ExceptionHugin {
        NodeList nodes = huginBN.getNodes();
        java.util.ListIterator it = nodes.listIterator();
        while (it.hasNext()) {
            Node node = (Node) it.next();
            System.out.println();
            System.out.println(node.getLabel() + " (" + node.getName() + ")");
            printBelief(node);
        }
    }

    @Override
    public void runInference() {
        try {
            this.huginBN.compile();
            huginBN.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
    }

    @Override
    public void setModel(BayesianNetwork model) {
        this.amidstBN = model;
        try {
            this.huginBN = BNConverterToHugin.convertToHugin(model);
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
    }

    @Override
    public BayesianNetwork getOriginalModel() {
        return amidstBN;
    }

    @Override
    public void setEvidence(Assignment assignment) {

        ((HashMapAssignment)assignment).entrySet().stream()
                .forEach(entry -> {
                    try {
                        this.setVarEvidence(entry.getKey(), entry.getValue().doubleValue());
                    } catch (ExceptionHugin exceptionHugin) {
                        exceptionHugin.printStackTrace();
                    }
                });
    }

    @Override
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {

        try {
            Node huginNode = huginBN.getNodeByName(var.getName());

            if (var.isMultinomial()) {
                Multinomial dist = new Multinomial(var);
                for(int i=0;i<var.getNumberOfStates();i++){
                    dist.setProbabilityOfState(i, ((DiscreteNode) huginNode).getBelief(i));
                }
                return (E)dist;
            }
            else if (var.isNormal()) {
                Normal dist = new Normal(var);
                dist.setMean(((ContinuousChanceNode)huginNode).getMean());
                dist.setVariance(((ContinuousChanceNode) huginNode).getVariance());
                return (E)dist;
            }
            else {
                throw new IllegalArgumentException("Variable type not allowed.");
            }
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
        return null;
    }


    @Override
    public double getLogProbabilityOfEvidence() {
        throw new UnsupportedOperationException("Method not implemented");
    }

    @Override
    public void setSeed(int seed) {

    }

    public static void main(String args[]) throws ExceptionHugin {

        BayesianNetworkGenerator.setNumberOfDiscreteVars(2);
        BayesianNetworkGenerator.setNumberOfContinuousVars(2);

        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        BNWriterToHugin.saveToHuginFile(bn,"networks/inference.net");

        Variable DiscreteVar0 = bn.getStaticVariables().getVariableById(0);
        Variable GaussianVar0 = bn.getStaticVariables().getVariableById(1);
        Variable GaussianVar1 = bn.getStaticVariables().getVariableById(2);
        Variable ClassVar = bn.getStaticVariables().getVariableById(3);

        //---------------------------------------------------------------------------

        // SET THE EVIDENCE
        HashMapAssignment assignment = new HashMapAssignment(2);
        //assignment.setValue(ClassVar, 0.0);
        assignment.setValue(DiscreteVar0, 1.0);
        assignment.setValue(GaussianVar0, -2.0);
        //assignment.setValue(GaussianVar1, 0.0);

        //---------------------------------------------------------------------------

        // INFERENCE
        HuginInference inferenceForBN = new HuginInference();
        inferenceForBN.setModel(bn);
        inferenceForBN.setEvidence(assignment);
        inferenceForBN.runInference();

        //---------------------------------------------------------------------------

        // POSTERIOR DISTRIBUTION
        System.out.println((inferenceForBN.getPosterior(ClassVar)).toString());
        //System.out.println((inferenceForBN.getPosterior(DiscreteVar0)).toString());
        //System.out.println((inferenceForBN.getPosterior(GaussianVar0)).toString());
        //System.out.println((inferenceForBN.getPosterior(GaussianVar1)).toString());

        //---------------------------------------------------------------------------
    }
}
