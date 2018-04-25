package eu.amidst.core.examples.inference;


import eu.amidst.core.inference.InferenceEngine;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

/**
 * This example show how to perform inference in a Bayesian network model using the InferenceEngine static class.
 * This class aims to be a straigthfoward way to perform queries over a Bayesian network model.
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class InferenceEngineExample {

    public static void main(String[] args) throws Exception {

        //We first load the WasteIncinerator bayesian network which has multinomial and Gaussian variables.
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/simulated/WasteIncinerator.bn");

        //We recover the relevant variables for this example: Mout which is normally distributed, and W which is multinomial.
        Variable varMout = bn.getVariables().getVariableByName("Mout");
        Variable varW = bn.getVariables().getVariableByName("W");

        //Set the evidence.
        Assignment assignment = new HashMapAssignment(1);
        assignment.setValue(varW,0);

        //Then we query the posterior of
        System.out.println("P(Mout|W=0) = " + InferenceEngine.getPosterior(varMout, bn, assignment));

        //Or some more refined queries
        System.out.println("P(0.7<Mout<6.59 | W=0) = " + InferenceEngine.getExpectedValue(varMout, bn, v -> (0.7 < v && v < 6.59) ? 1.0 : 0.0 ));

    }

}