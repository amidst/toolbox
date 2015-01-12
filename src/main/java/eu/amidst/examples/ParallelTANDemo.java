package eu.amidst.examples;

import COM.hugin.HAPI.ExceptionHugin;
import com.google.common.base.Stopwatch;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataOnStream;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.StaticDataOnMemoryFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.huginlink.ParallelTAN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by afa on 16/12/14.
 */
public class ParallelTANDemo {

    public static void demo1() throws ExceptionHugin, IOException {

        //It needs GBs, so avoid putting this file in a Dropbox folder!!!!
        String dataFile = new String("./datasets/Pigs.arff");


        BayesianNetwork bn = BayesianNetworkLoader.loadFromHugin("networks/Pigs.net");
        //int sampleSize = 100000;
        //BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        //sampler.setParallelMode(true);
        //sampler.sampleToAnARFFFile(dataFile, sampleSize);

        ArrayList<Integer> vSamplesOnMemory = new ArrayList(Arrays.asList(5000));
        ArrayList<Integer> vNumCores = new ArrayList(Arrays.asList(1, 2, 3, 4));

        for (Integer samplesOnMemory : vSamplesOnMemory) {
            for (Integer numCores : vNumCores) {
                System.out.println("Learning TAN: " + samplesOnMemory + " samples on memory, " + numCores + "core/s ...");
                DataBase data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));

                ParallelTAN tan = new ParallelTAN();
                tan.setNumCores(numCores);
                tan.setNumSamplesOnMemory(samplesOnMemory);
                tan.setNameRoot(bn.getStaticVariables().getListOfVariables().get(0).getName());
                tan.setNameTarget(bn.getStaticVariables().getListOfVariables().get(1).getName());
                Stopwatch watch = Stopwatch.createStarted();
                BayesianNetwork model = tan.learnBN(data);
                System.out.println(watch.stop());
            }
        }
    }


    public static void demo2() throws ExceptionHugin, IOException {

        //It needs GBs, so avoid putting this file in a Dropbox folder!!!!
        String dataFile = new String("./datasets/RandomData.arff");
        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(2000);
        BayesianNetworkGenerator.setNumberOfStates(2);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(new Random(0));


        //BayesianNetwork bn = BayesianNetworkLoader.loadFromHugin("networks/Pigs.net");
        int sampleSize = 10000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setParallelMode(true);
        sampler.sampleToAnARFFFile(dataFile, sampleSize);

        int samplesOnMemory = 1000;
        int numCores=1;
        System.out.println("Learning TAN: " + bn.getStaticVariables().getNumberOfVars() +" variables, " + sampleSize +" samples on disk, "+ samplesOnMemory + " samples on memory, "+  numCores + " core(s) ...");
        DataBase data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));

        ParallelTAN tan = new ParallelTAN();
        tan.setParallelMode(false);
        tan.setNumSamplesOnMemory(samplesOnMemory);
        tan.setNameRoot(bn.getStaticVariables().getListOfVariables().get(0).getName());
        tan.setNameTarget(bn.getStaticVariables().getListOfVariables().get(1).getName());
        BayesianNetwork model = tan.learnBN(data);
        System.out.println();

        numCores=Runtime.getRuntime().availableProcessors();
        System.out.println("Learning TAN: " + bn.getStaticVariables().getNumberOfVars() +" variables, " + sampleSize +" samples on disk, "+ samplesOnMemory + " samples on memory, "+  numCores + " core(s) ...");
        data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));

        tan = new ParallelTAN();
        tan.setParallelMode(true);
        tan.setNumSamplesOnMemory(samplesOnMemory);
        tan.setNameRoot(bn.getStaticVariables().getListOfVariables().get(0).getName());
        tan.setNameTarget(bn.getStaticVariables().getListOfVariables().get(1).getName());
        model = tan.learnBN(data);


    }

    public static void main(String[] args) throws ExceptionHugin, IOException {
        ParallelTANDemo.demo2();
    }
}