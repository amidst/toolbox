package eu.amidst.tutorials.huginsa2016.examples;



import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.FactoredFrontierForDBN;
import eu.amidst.dynamic.inference.InferenceAlgorithmForDBN;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.latentvariablemodels.dynamicmodels.DynamicModel;
import eu.amidst.latentvariablemodels.dynamicmodels.HiddenMarkovModel;

/**
 * Created by rcabanas on 23/05/16.
 */
public class DynamicModelInference {

    public static void main(String[] args) {

        //Load the datastream
        String filename = "datasets/simulated/exampleDS_d0_c5.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(filename);

        //Learn the model
        DynamicModel model = new HiddenMarkovModel(data.getAttributes());
        ((HiddenMarkovModel)model).setNumStatesHiddenVar(4);
        model.setWindowSize(200);
        model.updateModel(data);
        DynamicBayesianNetwork dbn = model.getModel();

        System.out.println(dbn);



        //Testing dataset
        String filenamePredict = "datasets/simulated/exampleDS_d0_c5_small.arff";
        DataStream<DynamicDataInstance> dataPredict = DynamicDataStreamLoader.loadFromFile(filenamePredict);

        //Select the inference algorithm
        InferenceAlgorithmForDBN infer = new FactoredFrontierForDBN(new VMP()); // new ImportanceSampling(),  new VMP(),
        infer.setModel(dbn);


        Variable varTarget = dbn.getDynamicVariables().getVariableByName("discreteHiddenVar");
        UnivariateDistribution posterior = null;

        //Classify each instance
        int t = 0;
        for (DynamicDataInstance instance : dataPredict) {

            infer.addDynamicEvidence(instance);
            infer.runInference();

           // posterior = infer.getFilteredPosterior(varTarget);
           // System.out.println("t="+t+", P(discreteHiddenVar | Evidence)  = " + posterior);

            posterior = infer.getPredictivePosterior(varTarget, 5);
            //Display the output
            System.out.println("t="+t+"+5, P(discreteHiddenVar | Evidence)  = " + posterior);


            t++;
        }





    }


}
