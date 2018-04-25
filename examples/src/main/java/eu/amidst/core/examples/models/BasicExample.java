package eu.amidst.core.examples.models;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.Model;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.LatentClassificationModel;

/**
 * Created by rcabanas on 13/04/2018.
 */
public class BasicExample {
    public static void main(String[] args) throws Exception {

        String datapath = "./datasets/simulated/";

        System.out.println("init");
        DataStream data = DataStreamLoader.open(datapath+"codrnaNorm_100k_1.arff");


        Model model = new LatentClassificationModel(data.getAttributes())
                .setClassName("codrna_Y")
                .setNumContinuousHidden(1)
                .setNumStatesHidden(2)
                .setWindowSize(1000);




        for(int i=1; i<=5; i++) {
            if (i > 1) data = DataStreamLoader.open(datapath+"codrnaNorm_100k_"+i+".arff");
            model.updateModel(data);
            System.out.println(model.getModel());

        }


        BayesianNetwork bn = model.getModel();

        //Variabeles of interest and evidence
        Variable varTarget = bn.getVariables().getVariableByName("codrna_Y");
        Variable varObserved = null;
        Assignment assignment = new HashMapAssignment(1);
        varObserved = bn.getVariables().getVariableByName("codrna_X1");
        assignment.setValue(varObserved,0.7);


        // Inference algorithm
        InferenceAlgorithm infer = new VMP();
        infer.setModel(bn);
        infer.setEvidence(assignment);

        //Query
        infer.runInference();
        Distribution p = infer.getPosterior(varTarget);
        System.out.println("P(codrna_Y|codrna_X1=0.7) = "+p);


    }
}
