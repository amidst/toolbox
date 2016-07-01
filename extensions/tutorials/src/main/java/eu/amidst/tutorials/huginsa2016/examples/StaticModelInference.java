package eu.amidst.tutorials.huginsa2016.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;
import eu.amidst.latentvariablemodels.staticmodels.Model;

/**
 * Created by rcabanas on 23/05/16.
 */
public class StaticModelInference {

    public static void main(String[] args) {

        //Load the datastream
        String filename = "datasets/simulated/exampleDS_d0_c5.arff";
        DataStream<DataInstance> data = DataStreamLoader.open(filename);

        //Learn the model
        Model model = new FactorAnalysis(data.getAttributes());
        ((FactorAnalysis)model).setNumberOfLatentVariables(3);
        model.updateModel(data);
        BayesianNetwork bn = model.getModel();

        System.out.println(bn);


        //Variabeles of interest
        Variable varTarget = bn.getVariables().getVariableByName("LatentVar2");
        Variable varObserved = null;

        //we set the evidence
        Assignment assignment = new HashMapAssignment(2);
        varObserved = bn.getVariables().getVariableByName("GaussianVar1");
        assignment.setValue(varObserved,6.5);

        //we set the algorithm
        InferenceAlgorithm infer = new ImportanceSampling(); //new HuginInference(); //new VMP();
        infer.setModel(bn);
        infer.setEvidence(assignment);

        //query
        infer.runInference();
        Distribution p = infer.getPosterior(varTarget);
        System.out.println("P(LatentVar2|GaussianVar1=6.5) = "+p);



    }

}
