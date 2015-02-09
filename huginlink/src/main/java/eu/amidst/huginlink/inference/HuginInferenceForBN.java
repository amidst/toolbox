package eu.amidst.huginlink.inference;

import COM.hugin.HAPI.*;
import eu.amidst.core.distribution.*;
import eu.amidst.core.inference.InferenceAlgorithmForBN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.converters.BNConverterToHugin;
import eu.amidst.huginlink.io.BNWriterToHugin;

/**
 * Created by afa on 9/2/15.
 */
public class HuginInferenceForBN implements InferenceAlgorithmForBN {

    BayesianNetwork amidstBN;
    Domain huginBN;

    private void setVarEvidence(Variable n, long value) throws ExceptionHugin {
        if (n.isMultinomial()){
            ((DiscreteNode)huginBN.getNodeByName(n.getName())).selectState(value);
        }
        else if (n.isGaussian()) {
            ((ContinuousChanceNode)huginBN.getNodeByName(n.getName())).enterValue(value);
        }
        else {
            throw new IllegalArgumentException("Variable type not allowed.");
        }
    }

    public static void printBelief(Node node) throws ExceptionHugin {

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

    public static void printBeliefs (Domain domain) throws ExceptionHugin {

        NodeList nodes = domain.getNodes();
        java.util.ListIterator it = nodes.listIterator();
        while (it.hasNext()) {
            Node node = (Node) it.next();
            System.out.println();
            System.out.println(node.getLabel() + " (" + node.getName() + ")");
            printBelief(node);
        }
    }

    @Override
    public void compileModel() {
        try {
            this.huginBN.compile();
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
    public BayesianNetwork getModel() {
        return amidstBN;
    }

    @Override
    public void setEvidence(Assignment assignment) {

        ((HashMapAssignment)assignment).entrySet().stream()
                .forEach(entry -> {
                    try {
                        this.setVarEvidence(entry.getKey(), entry.getValue().longValue());
                    } catch (ExceptionHugin exceptionHugin) {
                        exceptionHugin.printStackTrace();
                    }
                });
    }

    @Override
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {

        try {
            huginBN.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);
            Node huginNode = huginBN.getNodeByName(var.getName());

            if (var.isMultinomial()) {
                Multinomial dist = new Multinomial(var);
                for(int i=0;i<var.getNumberOfStates();i++){
                    dist.setProbabilityOfState(i, ((DiscreteNode) huginNode).getBelief(i));
                }
                return (E)dist;
            }
            else if (var.isGaussian()) {
                Normal dist = new Normal(var);
                dist.setMean(((ContinuousChanceNode)huginNode).getMean());
                dist.setSd(Math.sqrt(((ContinuousChanceNode) huginNode).getVariance()));
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
        HuginInferenceForBN inferenceForBN = new HuginInferenceForBN();
        inferenceForBN.setModel(bn);
        inferenceForBN.setEvidence(assignment);
        inferenceForBN.compileModel();

        //---------------------------------------------------------------------------

        // POSTERIOR DISTRIBUTION
        System.out.println((inferenceForBN.getPosterior(ClassVar)).toString());
        //System.out.println((inferenceForBN.getPosterior(DiscreteVar0)).toString());
        //System.out.println((inferenceForBN.getPosterior(GaussianVar0)).toString());
        //System.out.println((inferenceForBN.getPosterior(GaussianVar1)).toString());

        //---------------------------------------------------------------------------
    }
}
