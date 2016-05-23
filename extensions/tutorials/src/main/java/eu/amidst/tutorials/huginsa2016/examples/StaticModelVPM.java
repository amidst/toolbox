package eu.amidst.tutorials.huginsa2016.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;

/**
 * Created by rcabanas on 23/05/16.
 */
public class StaticModelVPM {

    public static void main(String[] args) {

        //Load the datastream
        String filename = "datasets/simulated/exampleDS_d0_c5.arff";
        DataStream<DataInstance> data = DataStreamLoader.openFromFile(filename);

        //Learn the model
        FactorAnalysis model = new FactorAnalysis(data.getAttributes());
        model.setNumberOfLatentVariables(3);
        model.setWindowSize(200);
        model.updateModel(data);
        BayesianNetwork bn = model.getModel();

        System.out.println(bn);


        //Initialize the inference algorithm
        InferenceAlgorithm inferenceAlgorithm = new VMP();
        inferenceAlgorithm.setModel(bn);

        Variable varTarget = bn.getVariables().getVariableByName("LatentVar2");
        Variable varObserved = null;


        //we set the evidence
        Assignment assignment = new HashMapAssignment(2);

        varObserved = bn.getVariables().getVariableByName("GaussianVar1");
        assignment.setValue(varObserved,6.5);
        inferenceAlgorithm.setEvidence(assignment);

        //Then we run inference
        inferenceAlgorithm.runInference();

        //Then we query the posterior of
        System.out.println("P(LatentVar2|GaussianVar1=6.5) = " + inferenceAlgorithm.getPosterior(varTarget));





    }

}
