package eu.amidst.examples;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DynamicDataInstance;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.learning.DynamicNaiveBayesClassifier;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.io.IOException;
import java.util.Random;

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
        sampler.setParallelMode(true);
        String file = "./datasets/randomdata.arff";
        sampler.sampleToAnARFFFile(file,sampleSize);

        DataBase<DynamicDataInstance> data = new DynamicDataOnDiskFromFile(new ARFFDataReader(file));

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork nbClassifier = model.getDynamicBNModel();
        System.out.println(nbClassifier.toString());


    }
}
