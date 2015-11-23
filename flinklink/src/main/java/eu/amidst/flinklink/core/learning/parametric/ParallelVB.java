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
package eu.amidst.flinklink.core.learning.parametric;


import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.learning.parametric.bayesian.DataPosterior;
import eu.amidst.core.learning.parametric.bayesian.DataPosteriorAssignment;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.core.utils.Serialization;
import org.apache.flink.api.common.aggregators.ConvergenceCriterion;
import org.apache.flink.api.common.aggregators.DoubleSumAggregator;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.functions.RichReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.IterativeDataSet;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.DoubleValue;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This class implements the {@link ParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class ParallelVB implements ParameterLearningAlgorithm, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    public static String PRIOR="PRIOR";
    public static String SVB="SVB";
    public static String LATENT_VARS="LATENT_VARS";

    /**
     * Represents the {@link DataFlink} used for learning the parameters.
     */
    protected DataFlink<DataInstance> dataFlink;

    /**
     * Represents the directed acyclic graph {@link DAG}.
     */
    protected DAG dag;

    protected SVB svb;

    protected int batchSize = 100;

    protected int maximumGlobalIterations = 10;

    protected int maximumLocalIterations = 100;

    protected double globalThreshold = 0.01;

    public ParallelVB(){
        this.svb = new SVB();
    }

    public void setGlobalThreshold(double globalThreshold) {
        this.globalThreshold = globalThreshold;
    }

    public void setMaximumGlobalIterations(int maximumGlobalIterations) {
        this.maximumGlobalIterations = maximumGlobalIterations;
    }

    public void setMaximumLocalIterations(int maximumLocalIterations) {
        this.maximumLocalIterations = maximumLocalIterations;
    }
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public SVB getSVB() {
        return svb;
    }

    public void initLearning() {
        //this.svb.getPlateuStructure().getVMP().setMaxIter(this.maximumLocalIterations);
        this.svb.setDAG(this.dag);
        this.svb.setWindowsSize(batchSize);
        this.svb.initLearning();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataFlink(DataFlink<DataInstance> data) {
        this.dataFlink = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogMarginalProbability() {
        throw new UnsupportedOperationException("Method not implemented yet");
    }

    public DataSet<DataPosteriorAssignment> computePosteriorAssignment(List<Variable> latentVariables){

        Attribute seq_id = this.dataFlink.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        try{
            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));
            config.setBytes(LATENT_VARS, Serialization.serializeObject(latentVariables));

            return this.dataFlink
                    .getBatchedDataSet(this.batchSize)
                    .flatMap(new ParallelVBMapInferenceAssignment())
                    .withParameters(config);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }

    public DataSet<DataPosterior> computePosterior(List<Variable> latentVariables){

        Attribute seq_id = this.dataFlink.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        try{
            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));
            config.setBytes(LATENT_VARS, Serialization.serializeObject(latentVariables));

            return this.dataFlink
                    .getBatchedDataSet(this.batchSize)
                    .flatMap(new ParallelVBMapInference())
                    .withParameters(config);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }

    public DataSet<DataPosterior> computePosterior(){

        Attribute seq_id = this.dataFlink.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        try{
            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));

            return this.dataFlink
                    .getBatchedDataSet(this.batchSize)
                    .flatMap(new ParallelVBMapInference())
                    .withParameters(config);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }
    public void updateModel(DataFlink<DataInstance> dataUpdate){

        try{
            final ExecutionEnvironment env = dataUpdate.getDataSet().getExecutionEnvironment();

            // get input data
            CompoundVector parameterPrior = this.svb.getNaturalParameterPrior();

            DataSet<CompoundVector> paramSet = env.fromElements(parameterPrior);

            // set number of bulk iterations for KMeans algorithm
            IterativeDataSet<CompoundVector> loop = paramSet.iterate(maximumGlobalIterations)
                    .registerAggregationConvergenceCriterion("ELBO_" + this.dag.getName(), new DoubleSumAggregator(), new ConvergenceELBO(this.globalThreshold));

            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));

            //We add an empty batched data set to emit the updated prior.
            DataOnMemory<DataInstance> emtpyBatch = new DataOnMemoryListContainer<DataInstance>(dataUpdate.getAttributes());
            DataSet<DataOnMemory<DataInstance>> unionData =
                    dataUpdate.getBatchedDataSet(this.batchSize)
                    .union(env.fromCollection(Arrays.asList(emtpyBatch),
                                              TypeExtractor.getForClass((Class<DataOnMemory<DataInstance>>) Class.forName("eu.amidst.core.datastream.DataOnMemory"))));

            DataSet<CompoundVector> newparamSet =
                    unionData
                    .map(new ParallelVBMap())
                    .withParameters(config)
                    .withBroadcastSet(loop, "VB_PARAMS_" + this.dag.getName())
                    .reduce(new ParallelVBReduce());

            // feed new centroids back into next iteration
            DataSet<CompoundVector> finlparamSet = loop.closeWith(newparamSet);

            parameterPrior.sum(finlparamSet.collect().get(0));
            this.svb.updateNaturalParameterPrior(parameterPrior);
        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {
        this.initLearning();
        this.updateModel(this.dataFlink);
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
        this.svb.setSeed(seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        return this.svb.getLearntBayesianNetwork();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutput(boolean activateOutput) {
        this.svb.setOutput(activateOutput);
    }


    public static class ParallelVBMap extends RichMapFunction<DataOnMemory<DataInstance>, CompoundVector> {

        DoubleSumAggregator elbo;

        SVB svb;

        @Override
        public CompoundVector map(DataOnMemory<DataInstance> dataBatch) throws Exception {

            if (dataBatch.getNumberOfDataInstances()==0){
                //System.out.println(this.svb.getLearntBayesianNetwork().toString());
                return this.svb.getNaturalParameterPrior();
            }else {
                SVB.BatchOutput out = svb.updateModelOnBatchParallel(dataBatch);
                //System.out.println("DIFF 36:" + out.getVector().getVectorByPosition(36).get(0)+", "+out.getVector().getVectorByPosition(36).get(1));
                //System.out.println("DIFF 37:" + out.getVector().getVectorByPosition(37).get(0)+", "+out.getVector().getVectorByPosition(37).get(1));

                elbo.aggregate(out.getElbo() / svb.getPlateuStructure().getVMP().getNodes().size());
                return out.getVector();
            }

        }


        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            String bnName = parameters.getString(BN_NAME, "");
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));


            Collection<CompoundVector> collection = getRuntimeContext().getBroadcastVariable("VB_PARAMS_" + bnName);
            CompoundVector updatedPrior = collection.iterator().next();

            //System.out.println("Prior 36\t" + updatedPrior.getVectorByPosition(36).get(0)+"\t"+updatedPrior.getVectorByPosition(36).get(1));
            //System.out.println("Prior 37:" + updatedPrior.getVectorByPosition(37).get(0)+", "+updatedPrior.getVectorByPosition(37).get(1));

            svb.updateNaturalParameterPrior(updatedPrior);

            //System.out.println("PriorP 36\t" + svb.getNaturalParameterPrior().getVectorByPosition(36).get(0) + "\t" + svb.getNaturalParameterPrior().getVectorByPosition(36).get(1));

            elbo = getIterationRuntimeContext().getIterationAggregator("ELBO_"+bnName);
        }
    }

    public static class ParallelVBMapInferenceAssignment extends RichFlatMapFunction<DataOnMemory<DataInstance>, DataPosteriorAssignment> {

        List<Variable> latentVariables;
        SVB svb;

        @Override
        public void flatMap(DataOnMemory<DataInstance> dataBatch, Collector<DataPosteriorAssignment> out) {
            for (DataPosteriorAssignment posterior: svb.computePosteriorAssignment(dataBatch, latentVariables)){
                out.collect(posterior);
            }
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));
            latentVariables = Serialization.deserializeObject(parameters.getBytes(LATENT_VARS, null));
        }
    }

    public static class ParallelVBMapInference extends RichFlatMapFunction<DataOnMemory<DataInstance>, DataPosterior> {

        List<Variable> latentVariables;
        SVB svb;

        @Override
        public void flatMap(DataOnMemory<DataInstance> dataBatch, Collector<DataPosterior> out) {
            if (latentVariables==null){
                for (DataPosterior posterior: svb.computePosterior(dataBatch)){
                    out.collect(posterior);
                }

            }else {
                for (DataPosterior posterior: svb.computePosterior(dataBatch, latentVariables)){
                    out.collect(posterior);
                }
            }
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));
            latentVariables = Serialization.deserializeObject(parameters.getBytes(LATENT_VARS, null));
        }
    }

    public static class ParallelVBReduce extends RichReduceFunction<CompoundVector> {
        @Override
        public CompoundVector reduce(CompoundVector value1, CompoundVector value2) throws Exception {
            value2.sum(value1);
            return value2;
        }
    }


    public static class ConvergenceELBO implements ConvergenceCriterion<DoubleValue>{

        final double threshold;
        double previousELBO = Double.NaN;

        public ConvergenceELBO(double threshold){
            this.threshold=threshold;
        }
        @Override
        public boolean isConverged(int iteration, DoubleValue value) {
            if (iteration==1) {
                previousELBO=value.getValue();
                return false;
            }else if (value.getValue() < (previousELBO - threshold)){
                //throw new IllegalStateException("Global bound is not monotonically increasing: "+ iteration +", " + value.getValue() +" < " + previousELBO);
                System.out.println("Global bound is not monotonically increasing: "+ iteration +", " + (value.getValue() +">" + previousELBO));
                this.previousELBO=value.getValue();
                return true;
            }else if (value.getValue()>(previousELBO+threshold)) {
                System.out.println("Global bound is monotonically increasing: "+ iteration +", " + (value.getValue() +">" + previousELBO));
                this.previousELBO=value.getValue();
                return false;
            }else {
                System.out.println("Global Convergence: "+ iteration +", " + (value.getValue() -previousELBO));
                return true;
            }
        }
    }
}