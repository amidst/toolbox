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

package eu.amidst.dynamic.learning.parametric;


import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.exponentialfamily.EF_DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;

import java.util.stream.Stream;

/**
 * This class implements the {@link eu.amidst.core.learning.parametric.ParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class ParallelMLMissingData implements ParameterLearningAlgorithm {

    /** Represents the batch size used for learning the parameters. */
    protected int windowsSize = 1000;

    /** Indicates the parallel processing mode, initialized here as {@code true}. */
    protected boolean parallelMode = false;

    /** Represents the {@link DataStream} used for learning the parameters. */
    protected DataStream<DynamicDataInstance> dataStream;

    /** Represents the directed acyclic graph {@link DynamicDAG}.*/
    protected DynamicDAG dag;

    /** Represents the data instance count. */
    protected AtomicDouble dataInstanceCount;

    /** Represents the sufficient statistics used for parameter learning. */
    protected DynamicPartialSufficientSatistics sumSS;

    /** Represents a {@link EF_DynamicBayesianNetwork} object */
    protected EF_DynamicBayesianNetwork efBayesianNetwork;

    /** Represents if the class is in debug mode*/
    protected boolean debug = false;

    /** Represents whether Laplace correction (i.e. MAP estimation) is used*/
    protected boolean laplace = true;


    /**
     * Sets whether Laplace correction (i.e. MAP estimation) is used
     * @param laplace, a boolean value.
     */
    public void setLaplace(boolean laplace) {
        this.laplace = laplace;
    }

    /**
     * Sets the debug mode of the class
     * @param debug a boolean setting whether to execute in debug mode or not.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Sets the windows size.
     * @param windowsSize the batch size.
     */
    public void setWindowsSize(int windowsSize) {
        windowsSize = windowsSize;
    }

    /**
     * Sets the windows size.
     */
    public int getWindowsSize() {
        return windowsSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initLearning() {
        efBayesianNetwork = new EF_DynamicBayesianNetwork(dag);
        if (laplace) {
            sumSS = DynamicPartialSufficientSatistics.createInitPartialSufficientStatistics(efBayesianNetwork);
            dataInstanceCount = new AtomicDouble(1.0); //Initial counts
        }else {
            sumSS = DynamicPartialSufficientSatistics.createZeroPartialSufficientStatistics(efBayesianNetwork);
            dataInstanceCount = new AtomicDouble(0.0); //Initial counts
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataOnMemory<DynamicDataInstance> batch) {

        this.sumSS.sum(batch.stream()
                    .map(dataInstance -> computeCountSufficientStatistics(this.efBayesianNetwork, dataInstance))
                    .reduce(DynamicPartialSufficientSatistics::sumNonStateless).get());

        dataInstanceCount.addAndGet(batch.getNumberOfDataInstances());

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataStream<DynamicDataInstance> dataStream) {

        Stream<DataOnMemory<DynamicDataInstance>> stream = null;
        if (parallelMode){
            stream = dataStream.parallelStreamOfBatches(windowsSize);
        }else{
            stream = dataStream.streamOfBatches(windowsSize);
        }


        dataInstanceCount = new AtomicDouble(0); //Initial count

        this.sumSS = stream
                .peek(batch -> {
                    dataInstanceCount.getAndAdd(batch.getNumberOfDataInstances());
                    if (debug) System.out.println("Parallel ML procesando "+(int)dataInstanceCount.get() +" instances");
                })
                .map(batch -> {
                    return batch.stream()
                            .map(dataInstance -> computeCountSufficientStatistics(this.efBayesianNetwork, dataInstance))
                            .reduce(DynamicPartialSufficientSatistics::sumNonStateless).get();
                })
                .reduce(DynamicPartialSufficientSatistics::sumNonStateless).get();

        if (laplace) {
            DynamicPartialSufficientSatistics initSS = DynamicPartialSufficientSatistics.createInitPartialSufficientStatistics(efBayesianNetwork);
            sumSS.sum(initSS);
        }

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataStream(DataStream<DynamicDataInstance> data) {
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

        Stream<DataOnMemory<DynamicDataInstance>> stream = null;
        if (parallelMode){
            stream = dataStream.parallelStreamOfBatches(windowsSize);
        }else{
            stream = dataStream.streamOfBatches(windowsSize);
        }


        dataInstanceCount = new AtomicDouble(0); //Initial count

        this.sumSS = stream
                .peek(batch -> {
                    dataInstanceCount.getAndAdd(batch.getNumberOfDataInstances());
                    if (debug) System.out.println("Parallel ML procesando "+(int)dataInstanceCount.get() +" instances");
                })
                .map(batch -> {
                    return batch.stream()
                            .map(dataInstance -> computeCountSufficientStatistics(this.efBayesianNetwork, dataInstance))
                            .reduce(DynamicPartialSufficientSatistics::sumNonStateless).get();
                })
                .reduce(DynamicPartialSufficientSatistics::sumNonStateless).get();

        if (laplace) {
            DynamicPartialSufficientSatistics initSS = DynamicPartialSufficientSatistics.createInitPartialSufficientStatistics(efBayesianNetwork);
            sumSS.sum(initSS);
        }


    }

    private static DynamicPartialSufficientSatistics computeCountSufficientStatistics(EF_DynamicBayesianNetwork bn, DynamicDataInstance dataInstance){


        if (dataInstance.getTimeID()==0) {
            return DynamicPartialSufficientSatistics.createPartialSufficientStatisticsTime0(eu.amidst.core.learning.parametric.ParallelMLMissingData.computeCountSufficientStatistics(bn.getBayesianNetworkTime0(),dataInstance));
        }else {
            return DynamicPartialSufficientSatistics.createPartialSufficientStatisticsTimeT(eu.amidst.core.learning.parametric.ParallelMLMissingData.computeCountSufficientStatistics(bn.getBayesianNetworkTimeT(),dataInstance));
        }

   }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDynamicDAG(DynamicDAG dag_) {
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
    public DynamicBayesianNetwork getLearntDBN() {
        //Normalize the sufficient statistics

        DynamicPartialSufficientSatistics partialSufficientSatistics = DynamicPartialSufficientSatistics.createZeroPartialSufficientStatistics(efBayesianNetwork);
        partialSufficientSatistics.copy(this.sumSS);
        partialSufficientSatistics.normalize();
        SufficientStatistics finalSS = efBayesianNetwork.createZeroSufficientStatistics();
        finalSS.sum(partialSufficientSatistics.getCompoundVector());

        efBayesianNetwork.setMomentParameters(finalSS);
        return efBayesianNetwork.toDynamicBayesianNetwork(dag);
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

    static class DynamicPartialSufficientSatistics {

        eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics time0;
        eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics timeT;

        public DynamicPartialSufficientSatistics(eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics time0, eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics timeT) {
            this.time0 = time0;
            this.timeT = timeT;
        }

        public static DynamicPartialSufficientSatistics createPartialSufficientStatisticsTime0(eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics partialSufficientSatistics) {
            return new DynamicPartialSufficientSatistics(partialSufficientSatistics,null);
        }

        public static DynamicPartialSufficientSatistics createPartialSufficientStatisticsTimeT(eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics partialSufficientSatistics) {
            return new DynamicPartialSufficientSatistics(null,partialSufficientSatistics);
        }

        public static DynamicPartialSufficientSatistics createInitPartialSufficientStatistics(EF_DynamicBayesianNetwork ef_bayesianNetwork){
            return new DynamicPartialSufficientSatistics(eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics.createInitPartialSufficientStatistics(ef_bayesianNetwork.getBayesianNetworkTime0()),
                    eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics.createInitPartialSufficientStatistics(ef_bayesianNetwork.getBayesianNetworkTimeT()));
        }

        public static DynamicPartialSufficientSatistics createZeroPartialSufficientStatistics(EF_DynamicBayesianNetwork ef_bayesianNetwork){
            return new DynamicPartialSufficientSatistics(eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics.createZeroPartialSufficientStatistics(ef_bayesianNetwork.getBayesianNetworkTime0()),
                    eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics.createZeroPartialSufficientStatistics(ef_bayesianNetwork.getBayesianNetworkTimeT()));
        }

        public eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics getTime0() {
            return time0;
        }

        public eu.amidst.core.learning.parametric.ParallelMLMissingData.PartialSufficientSatistics getTimeT() {
            return timeT;
        }

        public void normalize(){
            this.time0.normalize();
            this.timeT.normalize();
        }

        public void copy(DynamicPartialSufficientSatistics a){
            this.time0.copy(a.getTime0());
            this.timeT.copy(a.getTimeT());
        }
        public void sum(DynamicPartialSufficientSatistics a){
            if (a.getTime0()!=null)
                this.time0.sum(a.getTime0());
            if (a.getTimeT()!=null)
                this.timeT.sum(a.getTimeT());
        }

        public static DynamicPartialSufficientSatistics sumNonStateless(DynamicPartialSufficientSatistics a, DynamicPartialSufficientSatistics b) {

            if (b.getTime0()==null)
                b.time0=a.getTime0();
            else if (a.getTime0()!=null)
                b.getTime0().sum(a.getTime0());

            if (b.getTimeT()==null)
                b.timeT=a.getTimeT();
            else if (a.getTimeT()!=null)
                b.getTimeT().sum(a.getTimeT());

            return b;
        }

        public EF_DynamicBayesianNetwork.DynamiceBNCompoundVector getCompoundVector(){
            EF_DynamicBayesianNetwork.DynamiceBNCompoundVector vector = new EF_DynamicBayesianNetwork.DynamiceBNCompoundVector(this.getTime0().getCompoundVector(),this.getTimeT().getCompoundVector());
            vector.setIndicatorTime0(1);
            vector.setIndicatorTimeT(1);
            return vector;
        }
    }

}
