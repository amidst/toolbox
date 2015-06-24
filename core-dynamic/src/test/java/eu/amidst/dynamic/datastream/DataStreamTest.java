package eu.amidst.dynamic.datastream;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.DataOnMemoryFromFile;
import eu.amidst.core.datastream.filereaders.DataStreamFromFile;
import eu.amidst.dynamic.datastream.filereaders.DynamicDataOnMemoryFromFile;
import eu.amidst.dynamic.datastream.filereaders.DynamicDataStreamFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class DataStreamTest extends TestCase {

    @Test
    public void test1() {

        BayesianNetworkGenerator.setNumberOfGaussianVars(10);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(1, 2);
        BayesianNetworkGenerator.setSeed(0);
        final BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayes(2);

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setSeed(0);

        assertTrue(sampler.sampleToDataStream(100).streamOfBatches(2).count() == 50);

        sampler.sampleToDataStream(100).streamOfBatches(2).forEach( batch -> assertTrue(batch.getNumberOfDataInstances() == 2));

        assertTrue(sampler.sampleToDataStream(100).parallelStreamOfBatches(2).count() == 50);

        sampler.sampleToDataStream(100).parallelStreamOfBatches(2).forEach( batch -> assertTrue(batch.getNumberOfDataInstances() == 2));

    }

    @Test
    public void test2() {


        BayesianNetworkGenerator.setNumberOfGaussianVars(10);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(1, 2);
        BayesianNetworkGenerator.setSeed(0);
        final BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayes(2);

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setSeed(0);


        /*******************************************************************************/

        EF_BayesianNetwork efBayesianNetwork = new EF_BayesianNetwork(naiveBayes.getDAG());

        AtomicInteger dataInstanceCount = new AtomicInteger(0);

        SufficientStatistics sumSS = sampler.sampleToDataStream(1000).stream()
                .peek(w -> {
                    dataInstanceCount.getAndIncrement();
                })
                .map(efBayesianNetwork::getSufficientStatistics)
                .reduce(SufficientStatistics::sumVector).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efBayesianNetwork.setMomentParameters(sumSS);
        BayesianNetwork bn1 = efBayesianNetwork.toBayesianNetwork(naiveBayes.getDAG());

        /*******************************************************************************/

        efBayesianNetwork = new EF_BayesianNetwork(naiveBayes.getDAG());


        sumSS = sampler.sampleToDataStream(1000).streamOfBatches(10)
                .map( batch -> {
                    EF_BayesianNetwork efBayesianNetworkLocal = new EF_BayesianNetwork(naiveBayes.getDAG());
                    return batch.stream().map(efBayesianNetworkLocal::getSufficientStatistics).reduce(SufficientStatistics::sumVector).get();
                })
                .reduce(SufficientStatistics::sumVector).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efBayesianNetwork.setMomentParameters(sumSS);
        BayesianNetwork bn2 = efBayesianNetwork.toBayesianNetwork(naiveBayes.getDAG());

        /*******************************************************************************/


        efBayesianNetwork = new EF_BayesianNetwork(naiveBayes.getDAG());


        sumSS = sampler.sampleToDataStream(1000).parallelStreamOfBatches(10)
                .map( batch -> {
                    EF_BayesianNetwork efBayesianNetworkLocal = new EF_BayesianNetwork(naiveBayes.getDAG());
                    return batch.stream().map(efBayesianNetworkLocal::getSufficientStatistics).reduce(SufficientStatistics::sumVector).get();
                })
                .reduce(SufficientStatistics::sumVector).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efBayesianNetwork.setMomentParameters(sumSS);
        BayesianNetwork bn3 = efBayesianNetwork.toBayesianNetwork(naiveBayes.getDAG());

        /*******************************************************************************/

        assertTrue(bn1.equalBNs(bn2, 0.01));
        assertTrue(bn1.equalBNs(bn3, 0.01));

    }

    public static void example(DataStream<DataInstance> datastream){
        long nRows=0;
        for (DataInstance instance: datastream){
            nRows++;
        }

        System.out.println("Number of Rows: " + nRows);

        if (datastream.isRestartable()){
            datastream.restart();

            nRows = datastream.stream().count();

            System.out.println("Number of Rows: " + nRows);
        }
    }

    public void test3()  {

        ARFFDataReader reader = new ARFFDataReader();

        reader.loadFromFile("datasets/dataWeka/labor.arff");

        DataStream<DataInstance> data = new DataOnMemoryFromFile(reader);

        DataStream<DynamicDataInstance> dynamicdata = new DynamicDataOnMemoryFromFile(reader);

        DataStream<DataInstance> datastream = new DataStreamFromFile(reader);

        DataStream<DynamicDataInstance> dynamicdatastream = new DynamicDataStreamFromFile(reader);
    }

}