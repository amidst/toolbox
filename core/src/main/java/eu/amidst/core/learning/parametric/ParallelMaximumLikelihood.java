/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.core.learning.parametric;


import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class implements the {@link ParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class ParallelMaximumLikelihood implements ParameterLearningAlgorithm{

    /** Represents the batch size used for learning the parameters. */
    protected int batchSize = 1000;

    /** Indicates the parallel processing mode, initialized here as {@code true}. */
    protected boolean parallelMode = true;

    /** Represents the {@link DataStream} used for learning the parameters. */
    protected DataStream<DataInstance> dataStream;

    /** Represents the directed acyclic graph {@link DAG}.*/
    protected DAG dag;

    /** Represents the data instance count. */
    protected AtomicDouble dataInstanceCount;

    /** Represents the sufficient statistics used for parameter learning. */
    protected SufficientStatistics sumSS;

    /** Represents a {@link EF_BayesianNetwork} object */
    protected EF_BayesianNetwork efBayesianNetwork;


    /** Represents if the class is in debug mode*/
    protected boolean debug = true;


    /**
     * Sets the debug mode of the class
     * @param debug a boolean setting whether to execute in debug mode or not.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Sets the batch size.
     * @param batchSize_ the batch size.
     */
    public void setBatchSize(int batchSize_) {
        batchSize = batchSize_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initLearning() {
        dataInstanceCount = new AtomicDouble(0);
        efBayesianNetwork = new EF_BayesianNetwork(dag);
        sumSS = efBayesianNetwork.createZeroSufficientStatistics();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {

        this.sumSS = batch.stream()
                    .map(efBayesianNetwork::getSufficientStatistics)
                    .reduce(this.sumSS, SufficientStatistics::sumVector);

        dataInstanceCount.addAndGet(batch.getNumberOfDataInstances());

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataStream(DataStream<DataInstance> data) {
        this.dataStream=data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogMarginalProbability() {
        throw new UnsupportedOperationException("Method not implemented yet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {

        this.initLearning();

        Stream<DataOnMemory<DataInstance>> stream = null;
        if (parallelMode){
            stream = dataStream.parallelStreamOfBatches(batchSize);
        }else{
            stream = dataStream.streamOfBatches(batchSize);
        }

        dataInstanceCount = new AtomicDouble(0);

        sumSS = stream
                .peek(batch -> {
                    dataInstanceCount.getAndAdd(batch.getNumberOfDataInstances());
                    if (debug) System.out.println("Parallel ML procesando "+(int)dataInstanceCount.get() +" instances");
                })
                .map(batch -> {
                    SufficientStatistics ss = efBayesianNetwork.createZeroSufficientStatistics();
                    return batch.stream()
                            .map(efBayesianNetwork::getSufficientStatistics)
                            .reduce(ss, SufficientStatistics::sumVector);
                })
                .reduce(efBayesianNetwork.createZeroSufficientStatistics(), SufficientStatistics::sumVector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDAG(DAG dag_) {
        this.dag = dag_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSeed(int seed) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        //Normalize the sufficient statistics
        SufficientStatistics normalizedSS = efBayesianNetwork.createZeroSufficientStatistics();
        normalizedSS.copy(sumSS);
        normalizedSS.divideBy(dataInstanceCount.get());

        efBayesianNetwork.setMomentParameters(normalizedSS);
        return efBayesianNetwork.toBayesianNetwork(dag);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParallelMode(boolean parallelMode_) {
        parallelMode = parallelMode_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutput(boolean activateOutput) {

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
