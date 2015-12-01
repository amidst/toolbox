package eu.amidst.dynamic.examples.learning;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.learning.dynamic.StreamingVariationalBayesVMPForDBN;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;

import java.io.IOException;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 01/12/15.
 */
public class SVBforDBN {
    public static void main(String[] args) throws IOException {
        Random random = new Random(1);

        //We first generate a dynamic Bayesian network (NB structure with class and attributes temporally linked)
        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(3);
        DynamicBayesianNetwork dbnRandom = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random,2,true);

        //Sample dynamic data from the created dbn with random parameters
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbnRandom);
        sampler.setSeed(0);
        //Sample 3 sequences of 100K instances
        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(3,10000);

        /*Parameter Learning with Streaming variational Bayes VMP*/
        StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
        //We set the desired options for the svb
        svb.setWindowsSize(100);
        svb.setSeed(0);
        //If desired, we also set some options for the VMP
        VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
        vmp.setOutput(true);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        //We set the dynamicDAG, the data and start learning
        svb.setDynamicDAG(dbnRandom.getDynamicDAG());
        svb.setDataStream(data);
        svb.runLearning();

        //We get the learnt DBN
        DynamicBayesianNetwork dbnLearnt = svb.getLearntDBN();

        //We print the model
        System.out.println(dbnLearnt.toString());
    }

}
