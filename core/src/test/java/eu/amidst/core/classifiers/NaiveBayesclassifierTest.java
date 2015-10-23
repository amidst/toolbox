package eu.amidst.core.classifiers;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import org.junit.Test;

/**
 * Testing NaiveBayesClassifier
 */
public class NaiveBayesclassifierTest {

    @Test
    public void testingNBC(){

        BayesianNetworkGenerator.setNumberOfGaussianVars(100);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(7000, 10);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);


        int sampleSize = 100000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        DataStream<DataInstance> data =  sampler.sampleToDataStream(sampleSize);

        NaiveBayesClassifier model = new NaiveBayesClassifier();
        model.setClassName(data.getAttributes().getFullListOfAttributes().get(0).getName());
        model.learn(data);
        BayesianNetwork nbClassifier = model.getBNModel();

        //System.out.println(nbClassifier.toString());
    }
}
