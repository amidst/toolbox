package eu.amidst.tutorial.usingAmidst.examples;

import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;

/**
 * Created by rcabanas on 23/05/16.
 */
public class StaticModelInference {

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        BayesianNetwork bn  = BayesianNetworkLoader.loadFromFile("networks/simulated/exampleBN.bn");
        Variables variables = bn.getVariables();

        //Variabeles of interest
        Variable varTarget = variables.getVariableByName("LatentVar1");
        Variable varObserved = null;

        //we set the evidence
        Assignment assignment = new HashMapAssignment(2);
        varObserved = variables.getVariableByName("Income");
        assignment.setValue(varObserved,0.0);

        //we set the algorithm
        InferenceAlgorithm infer = new VMP(); //new HuginInference(); new ImportanceSampling();
        infer.setModel(bn);
        infer.setEvidence(assignment);

        //query
        infer.runInference();
        Distribution p = infer.getPosterior(varTarget);
        System.out.println("P(LatentVar1|Income=0.0) = "+p);

        //Or some more refined queries
        System.out.println("P(0.7<LatentVar1<6.59 |Income=0.0) = " + infer.getExpectedValue(varTarget, v -> (0.7 < v && v < 6.59) ? 1.0 : 0.0 ));

    }

}
