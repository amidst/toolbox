package eu.amidst.core.classifiers;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import org.junit.Assert;
import org.junit.Test;

/**
 * Testing NaiveBayesClassifier
 */
public class NaiveBayesClassifierTest {

    @Test
    public void testingNBC(){

        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(5, 2);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        Assert.assertEquals(5, bn.getNumberOfVars());

        int sampleSize = 100;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        DataStream<DataInstance> data =  sampler.sampleToDataStream(sampleSize);

        NaiveBayesClassifier model = new NaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
        model.learn(data);
        BayesianNetwork nbClassifier = model.getBNModel();

        System.out.println(nbClassifier.toString());
    }
}
