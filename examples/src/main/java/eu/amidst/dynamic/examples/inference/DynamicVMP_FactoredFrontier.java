package eu.amidst.dynamic.examples.inference;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.FactoredFrontierForDBN;
import eu.amidst.dynamic.inference.InferenceEngineForDBN;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;

import java.io.IOException;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 16/11/15.
 */
public class DynamicVMP_FactoredFrontier {
    public static void main(String[] args) throws IOException {

        Random random = new Random(1);

        //We first generate a dynamic Bayesian network (NB structure, only class is temporally linked)
        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(3);
        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random,2,false);

        DynamicDAG extendedNBDAG = new DynamicDAG(dbn.getDynamicVariables());


        //We add temporal links (1st order Markov Model) in the  predictive variables
        dbn.getDynamicVariables().getListOfDynamicVariables().stream()
                .filter(v -> !v.getName().equalsIgnoreCase("classVar"))
                .forEach(v -> { Variable varLinkInterface = dbn.getDynamicVariables().getInterfaceVariable(v);
                    extendedNBDAG.getParentSetTimeT(v).addParent(varLinkInterface);
                });


        DynamicBayesianNetwork extendedDBN = new DynamicBayesianNetwork(extendedNBDAG);

        //We create a dynamic dataset with 3 sequences for prediction
        DynamicBayesianNetworkSampler dynamicSampler = new DynamicBayesianNetworkSampler(extendedDBN);
        DataStream<DynamicDataInstance> dataPredict = dynamicSampler.sampleToDataBase(3,100);

        //We select the target variable for inference, in this case the class variable
        Variable classVar = extendedDBN.getDynamicVariables().getVariableByName("ClassVar");

        //We select VMP with the factored frontier algorithm as the Inference Algorithm
        FactoredFrontierForDBN FFalgorithm = new FactoredFrontierForDBN(new VMP());
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(FFalgorithm);

        //Then, we set the DBN model
        InferenceEngineForDBN.setModel(extendedDBN);

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

            //We show the output
            System.out.println("P(ClassVar|e) = "+posterior);
        }
    }
}
