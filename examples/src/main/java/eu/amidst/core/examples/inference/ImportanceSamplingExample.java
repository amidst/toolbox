package eu.amidst.core.examples.inference;


import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

/**
 *
 * This example we show how to perform inference on a general Bayesian network using an importance sampling
 * algorithm detailed in
 *
 * <i> Fung, R., and Chang, K. C. (2013). Weighing and integrating evidence for
 * stochastic simulation in Bayesian networks. arXiv preprint arXiv:1304.1504.
 * </i>
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class ImportanceSamplingExample {

    public static void main(String[] args) throws Exception {

        //We first load the WasteIncinerator bayesian network which has multinomial and Gaussian variables.
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/simulated/WasteIncinerator.bn");

        //We recover the relevant variables for this example: Mout which is normally distributed, and W which is multinomial.
        Variable varMout = bn.getVariables().getVariableByName("Mout");
        Variable varW = bn.getVariables().getVariableByName("W");

        //First we create an instance of a inference algorithm. In this case, we use the ImportanceSampling class.
        ImportanceSampling inferenceAlgorithm = new ImportanceSampling();
        //Then, we set the BN model
        inferenceAlgorithm.setModel(bn);

        System.out.println(bn.toString());

        //If it exists, we also set the evidence.
        Assignment assignment = new HashMapAssignment(1);
        assignment.setValue(varW,0);
        inferenceAlgorithm.setEvidence(assignment);

        //We can also set to be run in parallel on multicore CPUs
        inferenceAlgorithm.setParallelMode(true);

        //To perform more than one operation, data should be keep in memory
        inferenceAlgorithm.setKeepDataOnMemory(true);

        //Then we run inference
        inferenceAlgorithm.runInference();

        //Then we query the posterior of
        System.out.println("P(Mout|W=0) = " + inferenceAlgorithm.getPosterior(varMout));

        //Or some more refined queries
        System.out.println("P(0.7<Mout<6.59 | W=0) = " + inferenceAlgorithm.getExpectedValue(varMout, v -> (0.7 < v && v < 6.59) ? 1.0 : 0.0 ));

        //We can also compute the probability of the evidence
        System.out.println("P(W=0) = "+Math.exp(inferenceAlgorithm.getLogProbabilityOfEvidence()));

    }
}