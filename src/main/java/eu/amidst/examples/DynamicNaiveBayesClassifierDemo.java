package eu.amidst.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;
import eu.amidst.core.io.DynamicDataStreamLoader;
import eu.amidst.core.learning.DynamicNaiveBayesClassifier;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.io.IOException;

/**
 * Created by andresmasegosa on 15/01/15.
 */
public class DynamicNaiveBayesClassifierDemo {
    public static void main(String[] args) throws IOException {

        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        BayesianNetworkGenerator.setNumberOfStates(3);
        BayesianNetworkGenerator.setSeed(0);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        int sampleSize = 10000000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        String file = "./datasets/randomdata.arff";
        DataStream<DataInstance> dataStream = sampler.sampleToDataBase(sampleSize);
        ARFFDataWriter.writeToARFFFile(dataStream, file);

        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork nbClassifier = model.getDynamicBNModel();
        System.out.println(nbClassifier.toString());


    }
}
