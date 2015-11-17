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

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(3);
        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random,2,false);

        //TODO Fix so that it can work with Normal variables as well
        dbn.getDynamicVariables().getListOfDynamicVariables().stream()
                .filter(v -> !v.getName().equalsIgnoreCase("classVar"))
                .filter(v -> !v.isNormal())
                .forEach(v -> { Variable varLinkInterface = dbn.getDynamicVariables().getInterfaceVariable(v);
                    dbn.getDynamicDAG().getParentSetTimeT(v).addParent(varLinkInterface);
        });

        dbn.randomInitialization(random);

        DynamicBayesianNetworkSampler dynamicSampler = new DynamicBayesianNetworkSampler(dbn);

        DataStream<DynamicDataInstance> dataPredict = dynamicSampler.sampleToDataBase(1,10000);

        Variable classVar = dbn.getDynamicVariables().getVariableByName("ClassVar");

        UnivariateDistribution posterior = null;
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new eu.amidst.dynamic.inference.DynamicVMP());
        InferenceEngineForDBN.setModel(dbn);

        for (DynamicDataInstance instance : dataPredict) {
            if (instance.getTimeID()==0 && posterior != null) {
                InferenceEngineForDBN.reset();
            }
            instance.setValue(classVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            posterior = InferenceEngineForDBN.getFilteredPosterior(classVar);
        }
    }
}
