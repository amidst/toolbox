package eu.amidst.demos;

import COM.hugin.HAPI.ExceptionHugin;
import com.google.common.base.Stopwatch;

import eu.amidst.core.database.DataOnStream;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.huginlink.ParallelTAN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by afa on 16/12/14.
 */
public class ParallelTANDemo {

    public static void main(String[] args) throws ExceptionHugin, IOException {

        //It needs GBs, so avoid putting this file in a Dropbox folder!!!!
        String dataFile = new String("./datasets/Pigs.arff");


        BayesianNetwork bn = BayesianNetworkLoader.loadFromHugin("networks/Pigs.net");
        int sampleSize = 1000000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setParallelMode(true);
        sampler.sampleToAnARFFFile(dataFile,sampleSize);

        ArrayList<Integer> vSamplesOnMemory = new ArrayList(Arrays.asList(500000,600000,700000));
        ArrayList<Integer> vNumCores = new ArrayList(Arrays.asList(1,2,3,4));

        for (Integer samplesOnMemory : vSamplesOnMemory) {
            for (Integer numCores : vNumCores) {
                System.out.println("Learning TAN: "+ samplesOnMemory + " samples on memory, " + numCores +"core/s ...");
                DataOnStream data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));
                ParallelTAN tan= new ParallelTAN();
                tan.setNumCores(numCores);
                tan.setNumSamplesOnMemory(samplesOnMemory);
                Stopwatch watch = Stopwatch.createStarted();
                BayesianNetwork model = tan.learn(data, "p630400490", "p48124091");
                System.out.println(watch.stop());
            }
        }
    }
}
