package eu.amidst.core.learning;

import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.*;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public final class MaximumLikelihoodForBN {

    private static int batchSize = 1000;
    private static boolean parallelMode = true;

    public static int getBatchSize() {
        return batchSize;
    }

    public static void setBatchSize(int batchSize) {
        MaximumLikelihoodForBN.batchSize = batchSize;
    }

    public static boolean isParallelMode() {
        return parallelMode;
    }

    public static void setParallelMode(boolean parallelMode) {
        MaximumLikelihoodForBN.parallelMode = parallelMode;
    }

    public static BayesianNetwork learnParametersStaticModel(DAG dag, DataStream<DataInstance> dataStream) {

        EF_BayesianNetwork efBayesianNetwork = new EF_BayesianNetwork(dag);


        Stream<DataInstance> stream = null;
        if (parallelMode){
            stream = dataStream.parallelStream(batchSize);
        }else{
            stream = dataStream.stream();
        }

        AtomicInteger dataInstanceCount = new AtomicInteger(0);

        SufficientStatistics sumSS = stream
                .peek(w -> {
                    dataInstanceCount.getAndIncrement();
                })
                .map(efBayesianNetwork::getSufficientStatistics)
                .reduce(SufficientStatistics::sumVector).get();
                //.reduce(efBayesianNetwork.createZeroedSufficientStatistics(), SufficientStatistics::sumVector);

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efBayesianNetwork.setMomentParameters(sumSS);
        return efBayesianNetwork.toBayesianNetwork(dag);

    }

    public static BayesianNetwork learnParametersStaticModelFading(DAG dag, DataStream<DataInstance> dataStream, double fadingFactor, int windowSize) {

        EF_BayesianNetwork efBayesianNetwork = new EF_BayesianNetwork(dag);

        double nInstances=0;
        SufficientStatistics total = efBayesianNetwork.createZeroedSufficientStatistics();
        for (DataOnMemory<DataInstance> batch : dataStream.iterableOverBatches(windowSize)){
            SufficientStatistics batchSS = batch.stream()
                    .map(efBayesianNetwork::getSufficientStatistics)
                    .reduce(SufficientStatistics::sumVector).get();

            total.multiplyBy(fadingFactor);
            total.sum(batchSS);

            nInstances = nInstances*fadingFactor + batchSize;

        }

        //Normalize the sufficient statistics
        total.divideBy(nInstances);

        efBayesianNetwork.setMomentParameters(total);
        return efBayesianNetwork.toBayesianNetwork(dag);

    }


    public static void main(String[] args){

        List<ArrayVector> vectorList = IntStream.range(0,10).mapToObj(i -> {
            ArrayVector vec = new ArrayVector(2);
            vec.set(0, 1);
            vec.set(1, 1);
            return vec;
        }).collect(Collectors.toList());


        Vector out1 = vectorList.parallelStream()
                .reduce(new ArrayVector(2), (u, v) -> {
                    ArrayVector outvec = new ArrayVector(2);
                    outvec.sum(v);
                    outvec.sum(u);
                    return outvec;});


        Vector out2 = vectorList.parallelStream().reduce(new ArrayVector(2), (u, v) -> {u.sum(v); return u;});
        Vector out3 = vectorList.parallelStream().reduce(new ArrayVector(2), (u, v) -> {v.sum(u); return v;});

        System.out.println(out1.get(0) + ", " + out1.get(1));
        System.out.println(out2.get(0) + ", " + out2.get(1));
        System.out.println(out3.get(0) + ", " + out3.get(1));

        /*
        BayesianNetwork bn=null;

        int nlinks = bn.getDAG().getParentSets()
                .parallelStream()
                .mapToInt(parentSet -> parentSet.getNumberOfParents()).sum();

        nlinks=0;

        for (ParentSet parentSet: bn.getDAG().getParentSets()){
            nlinks+=parentSet.getNumberOfParents();
        }

        for (int i = 0; i < bn.getDAG().getParentSets().size(); i++) {
            nlinks+=bn.getDAG().getParentSets().get(i).getNumberOfParents();
        }*/


        int nSamples = 4000000;
        int sizeSS=1000000;
        int sizeSS2=100;

        double[][] sum = new double[sizeSS][];
        double[][] ss = new double[sizeSS][];

        for (int i = 0; i < 100; i++) {
            /*for (int j = 0; j < ss.length; j++) {
                    ss[j]=new double[sizeSS];
            }*/
            /*
            for (int j = 0; j < ss.length; j++) {
                for (int k = 0; k < ss[j].length; k++) {
                    ss[j][k]=1.0;//Math.random();
                }
            }


            for (int j = 0; j < ss.length; j++) {
                for (int k = 0; k < ss[j].length; k++) {
                    sum[j][k]+=ss[j][k];
                }
            }
*/

            class ArrayVector{
                double[] array;
                public ArrayVector(int size){
                    array = new double[size];
                }
                public double[] getArray(){
                    return this.array;
                }
            }

            Stopwatch watch = Stopwatch.createStarted();
            for (int j = 0; j < sizeSS ; j++) {
                    ArrayVector vex = new ArrayVector(sizeSS2);
                    ss[j]=vex.getArray();
            }
            System.out.println(watch.stop());

            watch = Stopwatch.createStarted();
            for (int j = 0; j < sizeSS ; j++) {
                ss[j]= new double[sizeSS2];
            }
            System.out.println(watch.stop());
            System.out.println();
        }


    }
}
