/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.learning.parametric;


import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class implements the {@link ParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class ParallelMLMissingData implements ParameterLearningAlgorithm{

    /** Represents the batch size used for learning the parameters. */
    protected int windowsSize = 1000;

    /** Indicates the parallel processing mode, initialized here as {@code true}. */
    protected boolean parallelMode = true;

    /** Represents the {@link DataStream} used for learning the parameters. */
    protected DataStream<DataInstance> dataStream;

    /** Represents the directed acyclic graph {@link DAG}.*/
    protected DAG dag;

    /** Represents the data instance count. */
    protected AtomicDouble dataInstanceCount;

    /** Represents the sufficient statistics used for parameter learning. */
    protected PartialSufficientSatistics sumSS;

    /** Represents a {@link EF_BayesianNetwork} object */
    protected EF_BayesianNetwork efBayesianNetwork;

    /** Represents if the class is in debug mode*/
    protected boolean debug = true;

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
        this.windowsSize = windowsSize;
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
        efBayesianNetwork = new EF_BayesianNetwork(dag);
        if (laplace) {
            sumSS = PartialSufficientSatistics.createInitPartialSufficientStatistics(efBayesianNetwork);
            dataInstanceCount = new AtomicDouble(1.0); //Initial counts
        }else {
            sumSS = PartialSufficientSatistics.createZeroPartialSufficientStatistics(efBayesianNetwork);
            dataInstanceCount = new AtomicDouble(0.0); //Initial counts
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {

        this.sumSS.sum(batch.stream()
                    .map(dataInstance -> computeCountSufficientStatistics(this.efBayesianNetwork, dataInstance))
                    .reduce(PartialSufficientSatistics::sumNonStateless).get());

        dataInstanceCount.addAndGet(batch.getNumberOfDataInstances());

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataStream<DataInstance> dataStream) {

        Stream<DataOnMemory<DataInstance>> stream = null;
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
                            .reduce(PartialSufficientSatistics::sumNonStateless).get();
                })
                .reduce(PartialSufficientSatistics::sumNonStateless).get();

        if (laplace) {
            PartialSufficientSatistics initSS = PartialSufficientSatistics.createInitPartialSufficientStatistics(efBayesianNetwork);
            sumSS.sum(initSS);
        }

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
                            .reduce(PartialSufficientSatistics::sumNonStateless).get();
                })
                .reduce(PartialSufficientSatistics::sumNonStateless).get();

        if (laplace) {
            PartialSufficientSatistics initSS = PartialSufficientSatistics.createInitPartialSufficientStatistics(efBayesianNetwork);
            sumSS.sum(initSS);
        }


    }

    public static PartialSufficientSatistics computeCountSufficientStatistics(EF_BayesianNetwork bn, DataInstance dataInstance){
        List<CountVector> list = bn.getDistributionList().stream().map(dist -> {
            if (Utils.isMissingValue(dataInstance.getValue(dist.getVariable())))
                return new CountVector();

            for (Variable var : dist.getConditioningVariables())
                if (Utils.isMissingValue(dataInstance.getValue(var)))
                    return new CountVector();

            return new CountVector(dist.getSufficientStatistics(dataInstance));
        }).collect(Collectors.toList());

        return new PartialSufficientSatistics(list);
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

        PartialSufficientSatistics partialSufficientSatistics = PartialSufficientSatistics.createZeroPartialSufficientStatistics(efBayesianNetwork);
        partialSufficientSatistics.copy(this.sumSS);
        partialSufficientSatistics.normalize();
        SufficientStatistics finalSS = efBayesianNetwork.createZeroSufficientStatistics();
        finalSS.sum(partialSufficientSatistics.getCompoundVector());

        efBayesianNetwork.setMomentParameters(finalSS);
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

    public static class PartialSufficientSatistics {

        List<CountVector> list;

        public PartialSufficientSatistics(List<CountVector> list) {
            this.list = list;
        }

        public static PartialSufficientSatistics createInitPartialSufficientStatistics(EF_BayesianNetwork ef_bayesianNetwork){
            return new PartialSufficientSatistics(ef_bayesianNetwork.getDistributionList().stream().map(w -> new CountVector(w.createInitSufficientStatistics())).collect(Collectors.toList()));
        }

        public static PartialSufficientSatistics createZeroPartialSufficientStatistics(EF_BayesianNetwork ef_bayesianNetwork){
            return new PartialSufficientSatistics(ef_bayesianNetwork.getDistributionList().stream().map(w -> new CountVector(w.createZeroSufficientStatistics())).collect(Collectors.toList()));
        }

        public void normalize(){
            list.stream().forEach(a -> a.normalize());
        }

        public void copy(PartialSufficientSatistics a){
            for (int i = 0; i < this.list.size(); i++) {
                this.list.get(i).copy(a.list.get(i));
            }
        }
        public void sum(PartialSufficientSatistics a){
            for (int i = 0; i < this.list.size(); i++) {
                this.list.get(i).sum(a.list.get(i));
            }
        }

        public static PartialSufficientSatistics sumNonStateless(PartialSufficientSatistics a, PartialSufficientSatistics b) {
            for (int i = 0; i < b.list.size(); i++) {
                b.list.get(i).sum(a.list.get(i));
            }
            return b;
        }

        public CompoundVector getCompoundVector(){
            List<Vector> ssList = this.list.stream().map(a -> a.sufficientStatistics).collect(Collectors.toList());
            return new CompoundVector(ssList);
        }
    }


    static class CountVector {

        SufficientStatistics sufficientStatistics;
        int count;

        public CountVector() {
            count=0;
            sufficientStatistics=null;
        }

        public CountVector(SufficientStatistics sufficientStatistics) {
            this.sufficientStatistics = sufficientStatistics;
            this.count=1;
        }

        public void normalize(){
            this.sufficientStatistics.divideBy(count);
        }

        public void copy(CountVector a){
            this.count = a.count;
            if (a.sufficientStatistics==null)
                this.sufficientStatistics = null;
            else if (this.sufficientStatistics==null)
                this.sufficientStatistics = a.sufficientStatistics;
            else
                this.sufficientStatistics.copy(a.sufficientStatistics);
        }

        public void sum(CountVector a){
            if (a.sufficientStatistics==null)
                return;

            this.count+=a.count;

            if (this.sufficientStatistics==null) {
                this.sufficientStatistics = a.sufficientStatistics;
            }else{
                this.sufficientStatistics.sum(a.sufficientStatistics);
            }
        }
    }
}
