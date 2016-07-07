package eu.amidst.tutorial.usingAmidst.examples;


import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.FactoredFrontierForDBN;
import eu.amidst.dynamic.inference.InferenceAlgorithmForDBN;
import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;

import java.io.IOException;

/**
 * Created by rcabanas on 23/05/16.
 */
public class DynamicModelInference {

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("networks/simulated/exampleDBN.dbn");

        System.out.println(dbn);

        //Testing dataset
        String filenamePredict = "datasets/simulated/cajamar.arff";
        DataStream<DynamicDataInstance> dataPredict = DynamicDataStreamLoader.loadFromFile(filenamePredict);

        //Select the inference algorithm
        InferenceAlgorithmForDBN infer = new FactoredFrontierForDBN(new ImportanceSampling()); // new ImportanceSampling(),  new VMP(),
        infer.setModel(dbn);


        Variable varTarget = dbn.getDynamicVariables().getVariableByName("discreteHiddenVar");
        UnivariateDistribution posterior = null;

        //Classify each instance
        int t = 0;
        for (DynamicDataInstance instance : dataPredict) {

            infer.addDynamicEvidence(instance);
            infer.runInference();

            posterior = infer.getFilteredPosterior(varTarget);
            System.out.println("t="+t+", P(discreteHiddenVar | Evidence)  = " + posterior);

            posterior = infer.getPredictivePosterior(varTarget, 2);
            //Display the output
            System.out.println("t="+t+"+5, P(discreteHiddenVar | Evidence)  = " + posterior);


            t++;
        }





    }


}
