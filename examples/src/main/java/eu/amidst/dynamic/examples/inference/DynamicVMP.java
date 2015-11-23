package eu.amidst.dynamic.examples.inference;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.InferenceEngineForDBN;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;

import java.io.IOException;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 16/11/15.
 */
public class DynamicVMP {
    public static void main(String[] args) throws IOException {

        Random random = new Random(1);

        //We first generate a dynamic Bayesian network (NB structure, only class is temporally linked)
        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(3);
        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random,2,false);

        //TODO Fix so that it can work with Normal variables as well
        //We add temporal links (1st order Markov Model) in the discrete predictive variables
        dbn.getDynamicVariables().getListOfDynamicVariables().stream()
                .filter(v -> !v.getName().equalsIgnoreCase("classVar"))
                .filter(v -> !v.isNormal())
                .forEach(v -> { Variable varLinkInterface = dbn.getDynamicVariables().getInterfaceVariable(v);
                    dbn.getDynamicDAG().getParentSetTimeT(v).addParent(varLinkInterface);
        });

        //We initialize the parameters of the network randomly
        dbn.randomInitialization(random);

        //We create a dynamic dataset with 3 sequences for prediction
        DynamicBayesianNetworkSampler dynamicSampler = new DynamicBayesianNetworkSampler(dbn);
        DataStream<DynamicDataInstance> dataPredict = dynamicSampler.sampleToDataBase(3,10000);

        //We select the target variable for inference, in this case the class variable
        Variable classVar = dbn.getDynamicVariables().getVariableByName("ClassVar");

        //We select DynamicVMP as the Inference Algorithm
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new eu.amidst.dynamic.inference.DynamicVMP());
        //Then, we set the DBN model
        InferenceEngineForDBN.setModel(dbn);

        UnivariateDistribution posterior = null;
        for (DynamicDataInstance instance : dataPredict) {
            //The InferenceEngineForDBN must be reset at the begining of each Sequence.
            if (instance.getTimeID()==0 && posterior != null) {
                InferenceEngineForDBN.reset();
            }
            //We also set the evidence.
            instance.setValue(classVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);

            //Then we run inference
            InferenceEngineForDBN.runInference();

            //Then we query the posterior of the target variable
            posterior = InferenceEngineForDBN.getFilteredPosterior(classVar);
        }
    }
}
