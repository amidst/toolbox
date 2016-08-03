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
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.learning.parametric.bayesian.*;
import eu.amidst.core.learning.parametric.bayesian.utils.DataPosterior;
import eu.amidst.core.learning.parametric.bayesian.utils.DataPosteriorAssignment;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.learning.parametric.bayesian.utils.TransitionMethod;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.learning.parametric.utils.IdenitifableModelling;
import eu.amidst.flinklink.core.learning.parametric.utils.ParameterIdentifiableModel;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.DecimalFormat;
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
public class DistributedVI implements ParameterLearningAlgorithm, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    static Logger logger = LoggerFactory.getLogger(DistributedVI.class);

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

    protected double localThreshold = 0.1;

    protected long timeLimit = -1;

    protected double globalELBO = Double.NaN;

    IdenitifableModelling idenitifableModelling = new ParameterIdentifiableModel();

    boolean randomStart = true;
    private int nBatches;


    public DistributedVI(){
        this.svb = new SVB();
    }


    public int getnBatches() {
        return nBatches;
    }

    public void setnBatches(int nBatches) {
        this.nBatches = nBatches;
    }

    public void setIdenitifableModelling(IdenitifableModelling idenitifableModelling) {
        this.idenitifableModelling = idenitifableModelling;
    }

    public void setPlateuStructure(PlateuStructure plateuStructure){
        this.svb.setPlateuStructure(plateuStructure);
    }

    public void setTransitionMethod(TransitionMethod transitionMethod){
        this.svb.setTransitionMethod(transitionMethod);
    }

    public void setGlobalThreshold(double globalThreshold) {
        this.globalThreshold = globalThreshold;
    }

    public void setLocalThreshold(double localThreshold) {
        this.localThreshold = localThreshold;
    }


    public void setMaximumGlobalIterations(int maximumGlobalIterations) {
        this.maximumGlobalIterations = maximumGlobalIterations;
    }

    public void setMaximumLocalIterations(int maximumLocalIterations) {
        this.maximumLocalIterations = maximumLocalIterations;
    }

    public void setTimeLimit(long timeLimit){
        this.timeLimit = timeLimit;
    }

    @Override
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }


    public SVB getSVB() {
        return svb;
    }

    public void initLearning() {
        //VMPParameter vmpParameter = new VMPParameter(this.svb.getPlateuStructure());
        //vmpParameter.setMaxGlobaIter(1);
        //this.svb.getPlateuStructure().setVmp(vmpParameter);
        this.svb.getPlateuStructure().getVMP().setMaxIter(this.maximumLocalIterations);
        this.svb.getPlateuStructure().getVMP().setThreshold(this.localThreshold);
        this.svb.setDAG(this.dag);
        this.svb.setWindowsSize(batchSize);
        this.svb.initLearning(); //Init learning is peformed in each mapper.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogMarginalProbability() {
        return this.globalELBO;
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

    @Override
    public double updateModel(DataFlink<DataInstance> dataUpdate){

        try{
            final ExecutionEnvironment env = dataUpdate.getDataSet().getExecutionEnvironment();

            // get input data
            CompoundVector parameterPrior = this.svb.getNaturalParameterPrior();

            DataSet<CompoundVector> paramSet = env.fromElements(parameterPrior);

            ConvergenceCriterion convergenceELBO;
            if(timeLimit == -1) {
                convergenceELBO = new ConvergenceELBO(this.globalThreshold, System.nanoTime());
            }
            else {
                convergenceELBO = new ConvergenceELBObyTime(this.timeLimit, System.nanoTime());
                this.setMaximumGlobalIterations(5000);
            }
            // set number of bulk iterations for KMeans algorithm
            IterativeDataSet<CompoundVector> loop = paramSet.iterate(maximumGlobalIterations)
                    .registerAggregationConvergenceCriterion("ELBO_" + this.dag.getName(), new DoubleSumAggregator(),convergenceELBO);

            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));

            //We add an empty batched data set to emit the updated prior.
            DataOnMemory<DataInstance> emtpyBatch = new DataOnMemoryListContainer<DataInstance>(dataUpdate.getAttributes());
            DataSet<DataOnMemory<DataInstance>> unionData = null;

            unionData =
                    dataUpdate.getBatchedDataSet(this.batchSize)
                            .union(env.fromCollection(Arrays.asList(emtpyBatch),
                                    TypeExtractor.getForClass((Class<DataOnMemory<DataInstance>>) Class.forName("eu.amidst.core.datastream.DataOnMemory"))));

            DataSet<CompoundVector> newparamSet =
                    unionData
                    .map(new ParallelVBMap(randomStart, idenitifableModelling))
                    .withParameters(config)
                    .withBroadcastSet(loop, "VB_PARAMS_" + this.dag.getName())
                    .reduce(new ParallelVBReduce());

            // feed new centroids back into next iteration
            DataSet<CompoundVector> finlparamSet = loop.closeWith(newparamSet);

            parameterPrior = finlparamSet.collect().get(0);

            this.svb.updateNaturalParameterPosteriors(parameterPrior);

            this.svb.updateNaturalParameterPrior(parameterPrior);

            if(timeLimit == -1)
                this.globalELBO = ((ConvergenceELBO)loop.getAggregators().getConvergenceCriterion()).getELBO();
            else
                this.globalELBO = ((ConvergenceELBObyTime)loop.getAggregators().getConvergenceCriterion()).getELBO();

            this.svb.applyTransition();

        }catch(Exception ex){
            throw new RuntimeException(ex.getMessage());
        }

        this.randomStart=false;

        return this.getLogMarginalProbability();
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


    public <E extends UnivariateDistribution> E getParameterPosterior(Variable parameter) {
            return this.svb.getParameterPosterior(parameter);
    }





    public static class ParallelVBMap extends RichMapFunction<DataOnMemory<DataInstance>, CompoundVector> {

        DoubleSumAggregator elbo;

        double basedELBO = -Double.MAX_VALUE;

        SVB svb;

        CompoundVector prior;

        CompoundVector initialPosterior;

        CompoundVector updatedPosterior;


        String bnName;


        IdenitifableModelling idenitifableModelling;

        boolean randomStart;

        double factor = 1;
        double learningRate = 0.7;
        double KL=0;

        public ParallelVBMap(boolean randomStart, IdenitifableModelling idenitifableModelling) {
            this.randomStart = randomStart;
            this.idenitifableModelling = idenitifableModelling;
        }


        @Override
        public CompoundVector map(DataOnMemory<DataInstance> dataBatch) throws Exception {

            if (dataBatch.getNumberOfDataInstances()==0){
                elbo.aggregate(basedELBO);
                return prior;//this.svb.getNaturalParameterPrior();
            }else {

                this.svb.updateNaturalParameterPosteriors(updatedPosterior);

                svb.getPlateuStructure().getNonReplictedNodes().forEach(node -> node.setActive(false));
                svb.setOutput(false);
                SVB.BatchOutput outElbo = svb.updateModelOnBatchParallel(dataBatch);
                svb.setOutput(true);

                if (Double.isNaN(outElbo.getElbo()))
                    throw new IllegalStateException("NaN elbo");

                elbo.aggregate(outElbo.getElbo());




                //Set Active Parameters
                int superstep = getIterationRuntimeContext().getSuperstepNumber() - 1;
                svb.getPlateuStructure()
                        .getNonReplictedNodes()
                        .forEach(node ->
                                node.setActive(this.idenitifableModelling.isActiveAtEpoch(node.getMainVariable(), superstep))
                        );

                if (superstep == 0){
                    this.svb.getPlateuStructure().setSeed(this.svb.getSeed());
                    this.svb.getPlateuStructure().resetQs();
                    this.svb.updateNaturalParameterPosteriors(updatedPosterior);
                }

                outElbo = svb.updateModelOnBatchParallel(dataBatch);


                if (Double.isNaN(outElbo.getElbo()))
                    throw new IllegalStateException("NaN elbo");


                KL += svb.getPlateuStructure().getNonReplictedNodes().mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum();



                return outElbo.getVector();
            }

        }

        @Override
        public void close() {

            double gradient = KL + 0.01/factor;

            factor = factor +learningRate*gradient;

            if (factor<0.01){
                factor=0.01;
            }

        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            bnName = parameters.getString(BN_NAME, "");
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));

            svb.initLearning();

            Collection<CompoundVector> collection = getRuntimeContext().getBroadcastVariable("VB_PARAMS_" + bnName);
            updatedPosterior = collection.iterator().next();


            if (prior!=null) {
                svb.updateNaturalParameterPrior(prior);
                svb.updateNaturalParameterPosteriors(updatedPosterior);
                basedELBO = svb.getPlateuStructure().getNonReplictedNodes().mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum();



                CompoundVector referencePrior = Serialization.deepCopy(updatedPosterior);
                referencePrior.multiplyBy(factor);
                svb.updateNaturalParameterPrior(referencePrior);

            }else{
                this.prior=Serialization.deepCopy(updatedPosterior);
                this.svb.updateNaturalParameterPrior(prior);
                if (randomStart) {
                    this.svb.getPlateuStructure().setSeed(this.svb.getSeed());
                    this.svb.getPlateuStructure().resetQs();
                    initialPosterior = Serialization.deepCopy(this.svb.getPlateuStructure().getPlateauNaturalParameterPosterior());
                    initialPosterior.sum(prior);
                }else{
                    initialPosterior=Serialization.deepCopy(svb.getNaturalParameterPrior());
                }

                this.svb.updateNaturalParameterPosteriors(initialPosterior);

                basedELBO = svb.getPlateuStructure().getNonReplictedNodes().mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum();

                updatedPosterior=initialPosterior;
            }



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
            svb.initLearning();
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
            svb.initLearning();
            latentVariables = Serialization.deserializeObject(parameters.getBytes(LATENT_VARS, null));
        }
    }

    public static class ParallelVBReduce extends RichReduceFunction<CompoundVector> {
        @Override
        public CompoundVector reduce(CompoundVector value1, CompoundVector value2) throws Exception {
/*            value2.sum(value1);
            return value2;
*/

            CompoundVector newValue  = Serialization.deepCopy(value1);
            newValue.sum(value2);
            return newValue;
        }
    }


    public static class ConvergenceELBO implements ConvergenceCriterion<DoubleValue>{

        final double threshold;
        double previousELBO = Double.NaN;
        long start;

        public ConvergenceELBO(double threshold, long start){
            this.threshold=threshold;
            this.start = start;
        }

        public double getELBO() {
            return previousELBO;
        }

        @Override
        public boolean isConverged(int iteration, DoubleValue value) {


            if (iteration==1)
                return false;

            iteration--;

            if (Double.isNaN(value.getValue()))
                throw new IllegalStateException("A NaN elbo");

            if (value.getValue()==Double.NEGATIVE_INFINITY)
                value.setValue(-Double.MAX_VALUE);

            double percentage = 100*(value.getValue() - previousELBO)/Math.abs(previousELBO);

            DecimalFormat df = new DecimalFormat("0.0000");

            if (iteration==1) {
                previousELBO=value.getValue();
                logger.info("Global bound at first iteration: 1,{},{} seconds",df.format(value.getValue()),
                        df.format((System.nanoTime() - start) / 1000000000.0));
                System.out.println("Global bound at first iteration: 1," + df.format(value.getValue())+ "," +
                        df.format((System.nanoTime() - start) / 1000000000.0) + " seconds");

                return false;
            }else if (percentage<0 && percentage < -threshold){
                logger.info("Global bound is not monotonically increasing: {},{},{}<{}",iteration, df.format(
                        percentage), df.format(value.getValue()), df.format(previousELBO));
                throw new IllegalStateException("Global bound is not monotonically increasing: "+ iteration +","+
                        df.format(percentage) +"," + df.format(value.getValue()) +" < " + df.format(previousELBO));
                //System.out.println("Global bound is not monotonically increasing: "+ iteration +", "+ percentage +
                // ", "+ (value.getValue() +">" + previousELBO));
                //this.previousELBO=value.getValue();
                //return false;
            }else if (percentage>0 && percentage>threshold) {
                logger.info("Global bound is monotonically increasing: {},{},{}>{},{} seconds",iteration,
                        df.format(percentage), df.format(value.getValue()), df.format(previousELBO),
                        df.format((System.nanoTime() - start) / 1000000000.0));
                System.out.println("Global bound is monotonically increasing: "+ iteration +","+df.format(percentage)+
                        "," + (df.format(value.getValue()) +">" + df.format(previousELBO))+ ","+
                        df.format((System.nanoTime() - start) / 1000000000.0) + " seconds");
                this.previousELBO=value.getValue();
                return false;
            }else {
                logger.info("Global bound Convergence: {},{},{},{} seconds",iteration,df.format(percentage),
                        df.format(value.getValue()), df.format((System.nanoTime() - start) / 1000000000.0));
                System.out.println("Global bound Convergence: "+ iteration +"," + df.format(percentage) + "," +
                        df.format(value.getValue())+ "," + df.format((System.nanoTime() - start) / 1000000000.0) +
                        " seconds");
                return true;
            }
        }
    }

    public static class ConvergenceELBObyTime implements ConvergenceCriterion<DoubleValue>{

        double previousELBO = Double.NaN;
        final double timeLimit;
        long start;

        public ConvergenceELBObyTime(double timeLimit, long start){
            this.start = start;
            this.timeLimit = timeLimit;
        }

        public double getELBO() {
            return previousELBO;
        }

        @Override
        public boolean isConverged(int iteration, DoubleValue value) {


            if (iteration==1)
                return false;

            iteration--;

            if (Double.isNaN(value.getValue()))
                throw new IllegalStateException("A NaN elbo");

            if (value.getValue()==Double.NEGATIVE_INFINITY)
                value.setValue(-Double.MAX_VALUE);

            double percentage = 100*(value.getValue() - previousELBO)/Math.abs(previousELBO);

            double timeIteration = (System.nanoTime() - start) / 1000000000.0;

            DecimalFormat df = new DecimalFormat("0.0000");

            if (iteration==1) {
                previousELBO=value.getValue();
                logger.info("Global bound at first iteration: 1,{},{} seconds",df.format(value.getValue()),
                        df.format((System.nanoTime() - start) / 1000000000.0));
                System.out.println("Global bound at first iteration: 1," + value.getValue()+ "," +
                        ((System.nanoTime() - start) / 1000000000.0) + " seconds");
                return false;
            }else if (percentage<-1){
                logger.info("Global bound is not monotonically increasing: {},{},{}<{}",iteration, df.format(
                        percentage), df.format(value.getValue()), df.format(previousELBO));
                throw new IllegalStateException("Global bound is not monotonically increasing: "+ iteration +","+
                        df.format(percentage) +"," + df.format(value.getValue()) +" < " + df.format(previousELBO));
            }else if (percentage>-1 && timeIteration < timeLimit) {
                logger.info("Global bound is monotonically increasing: {},{},{}>{},{} seconds",iteration,
                        df.format(percentage), df.format(value.getValue()), df.format(previousELBO),
                        df.format((System.nanoTime() - start) / 1000000000.0));

                System.out.println("Global bound is monotonically increasing: "+ iteration +","+percentage+
                        "," + (value.getValue()) +">" + previousELBO+ ","+
                        (System.nanoTime() - start) / 1000000000.0 + " seconds");

                this.previousELBO=value.getValue();
                return false;
            }else {
                logger.info("Global bound Convergence: {},{},{},{} seconds",iteration,df.format(percentage),
                        df.format(value.getValue()), df.format((System.nanoTime() - start) / 1000000000.0));

                System.out.println("Global bound Convergence: "+ iteration +"," + percentage + "," +
                        value.getValue()+ "," + (System.nanoTime() - start) / 1000000000.0 +
                        " seconds");
                return true;
            }
        }
    }
}