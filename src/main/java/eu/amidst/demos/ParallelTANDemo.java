package eu.amidst.demos;

import COM.hugin.HAPI.ExceptionHugin;
import com.google.common.base.Stopwatch;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.DataOnMemory;
import eu.amidst.core.database.DataOnStream;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.huginlink.ParallelTAN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetworkWriter;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.ReservoirSampling;

import java.io.IOException;

/**
 * Created by afa on 16/12/14.
 */
public class ParallelTANDemo {

    public static void main(String[] args) throws ExceptionHugin, IOException {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromHugin("networks/Pigs.net");
        int sampleSize = 100000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setParallelMode(true);
        sampler.sampleToAnARFFFile("datasets/PigsSample.arff",sampleSize);

        System.out.println("Number of variables: "+bn.getNumberOfVars());

        DataOnStream data = new StaticDataOnDiskFromFile(new WekaDataFileReader("datasets/PigsSample.arff"));

        ParallelTAN tan= new ParallelTAN();
        tan.setNumCores(4);
        tan.setNumSamplesOnMemory(10000);

        System.out.println("Learning TAN ...");
        BayesianNetwork model = tan.learn(data, "p630400490", "p48124091");
        BayesianNetworkWriter.saveToHuginFile(model,"TANFromPigSample.net");
    }







}
