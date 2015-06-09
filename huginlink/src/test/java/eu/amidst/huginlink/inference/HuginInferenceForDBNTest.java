package eu.amidst.huginlink.inference;

import com.google.common.primitives.Doubles;
import eu.amidst.corestatic.datastream.DataStream;
import eu.amidst.corestatic.datastream.DynamicDataInstance;
import eu.amidst.corestatic.distribution.UnivariateDistribution;
import eu.amidst.corestatic.inference.DynamicVMP;
import eu.amidst.corestatic.inference.InferenceEngineForDBN;
import eu.amidst.corestatic.io.DynamicBayesianNetworkLoader;
import eu.amidst.corestatic.io.DynamicBayesianNetworkWriter;
import eu.amidst.corestatic.io.DynamicDataStreamLoader;
import eu.amidst.corestatic.learning.dynamic.DynamicNaiveBayesClassifier;
import eu.amidst.corestatic.models.DynamicBayesianNetwork;
import eu.amidst.corestatic.utils.Utils;
import eu.amidst.corestatic.variables.Variable;

import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by afa on 1/3/15.
 */
public class HuginInferenceForDBNTest {

    ArrayList<Double> huginProbabilities;
    ArrayList<Double> amidstProbabilities;
    DynamicBayesianNetwork amidstDBN;
    UnivariateDistribution posterior;
    Variable defaultVar;
    DataStream<DynamicDataInstance> dataPredict;

    @Before
    public void setUp() throws IOException, ClassNotFoundException {

        //**************************************************************************************************************
        // LEARN A DYNAMIC BAYESIAN NETWORK
        //**************************************************************************************************************

        String file = "./datasets/bank_data_train.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);
        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 3);//We set -3 to account for time id and seq_id
        model.setParallelMode(true);
        model.learn(data);
        amidstDBN = model.getDynamicBNModel();
        DynamicBayesianNetworkWriter.saveToFile(amidstDBN, "networks/CajamarDBN.dbn");

        amidstDBN = DynamicBayesianNetworkLoader.loadFromFile("networks/CajamarDBN.dbn");
        defaultVar = amidstDBN.getDynamicVariables().getVariableByName("DEFAULT");

        //**************************************************************************************************************
        // DATA TO PREDICT
        //**************************************************************************************************************

        String filePredict = "./datasets/bank_data_predict.arff";
        dataPredict = DynamicDataStreamLoader.loadFromFile(filePredict);
    }

    @Test
    public void testFilteredPosterior() throws IOException, ClassNotFoundException {

        //**************************************************************************************************************
        // HUGIN - FILTERED POSTERIOR
        //**************************************************************************************************************

        posterior = null;
        huginProbabilities = new ArrayList();
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new HuginInferenceForDBN());
        InferenceEngineForDBN.setModel(amidstDBN);

        for (DynamicDataInstance instance : dataPredict) {
            if (instance.getTimeID()==0 && posterior != null) {
                huginProbabilities.add(posterior.getProbability(0.0));
                InferenceEngineForDBN.reset();
            }
            instance.setValue(defaultVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            posterior = InferenceEngineForDBN.getFilteredPosterior(defaultVar);
        }

        //**************************************************************************************************************
        // AMIDST - FILTERED POSTERIOR
        //**************************************************************************************************************

        posterior = null;
        amidstProbabilities = new ArrayList();
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new DynamicVMP());
        InferenceEngineForDBN.setModel(amidstDBN);

        for (DynamicDataInstance instance : dataPredict) {
            if (instance.getTimeID()==0 && posterior != null) {
                amidstProbabilities.add(posterior.getProbability(0.0));
                InferenceEngineForDBN.reset();
            }
            instance.setValue(defaultVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            posterior = InferenceEngineForDBN.getFilteredPosterior(defaultVar);
        }

        //**************************************************************************************************************
        //TODO the test passes only using a delta > 0.025 in the assert !!!!!
        assertArrayEquals(Doubles.toArray(huginProbabilities), Doubles.toArray(amidstProbabilities), 0.02);
        //**************************************************************************************************************
    }

    @Test
    public void testPredictivePosterior() throws IOException, ClassNotFoundException {

        //**************************************************************************************************************
        // HUGIN - PREDICTIVE POSTERIOR
        //**************************************************************************************************************

        posterior = null;
        huginProbabilities = new ArrayList();
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new HuginInferenceForDBN());
        InferenceEngineForDBN.setModel(amidstDBN);

        for (DynamicDataInstance instance : dataPredict) {
            if (instance.getTimeID()==0 && posterior != null) {
                huginProbabilities.add(posterior.getProbability(0.0));
                //System.out.println(posterior.getProbability(0.0));
                InferenceEngineForDBN.reset();
            }
            instance.setValue(defaultVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            posterior = InferenceEngineForDBN.getPredictivePosterior(defaultVar,2);
        }

        System.out.println("\n");

        //**************************************************************************************************************
        // AMIDST - PREDICTIVE POSTERIOR
        //**************************************************************************************************************

        posterior = null;
        amidstProbabilities = new ArrayList();
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new DynamicVMP());
        InferenceEngineForDBN.setModel(amidstDBN);

        for (DynamicDataInstance instance : dataPredict) {
            if (instance.getTimeID()==0 && posterior != null) {
                amidstProbabilities.add(posterior.getProbability(0.0));
                //System.out.println(posterior.getProbability(0.0));
                InferenceEngineForDBN.reset();
            }
            instance.setValue(defaultVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            posterior = InferenceEngineForDBN.getPredictivePosterior(defaultVar,2);
        }

        //**************************************************************************************************************
        //TODO the test passes only using a delta > 0.025 in the assert !!!!!
        assertArrayEquals(Doubles.toArray(huginProbabilities), Doubles.toArray(amidstProbabilities), 0.025);
        //**************************************************************************************************************
    }
}