package eu.amidst.examples;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.learning.NaiveBayesClassifier;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.util.Random;

/**
 * Created by andresmasegosa on 15/01/15.
 */
public class NaiveBayesClassifierDemo {
    public static void main(String[] args) {

        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(50000);
        BayesianNetworkGenerator.setNumberOfStates(10);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);
        BayesianNetworkGenerator.setSeed(0);

        int sampleSize = 100;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setParallelMode(true);
        DataBase data =  sampler.sampleToDataBase(sampleSize);

        NaiveBayesClassifier model = new NaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
        model.setParallelMode(true);
        model.learn(data);
        BayesianNetwork nbClassifier = model.getBNModel();
        System.out.println(nbClassifier.toString());

    }
}
