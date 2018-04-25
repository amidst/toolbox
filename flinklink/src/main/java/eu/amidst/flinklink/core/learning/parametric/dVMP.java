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
package eu.amidst.flinklink.core.learning.parametric;


import eu.amidst.core.constraints.Constraint;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.utils.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.learning.parametric.utils.GlobalvsLocalUpdate;
import eu.amidst.flinklink.core.learning.parametric.utils.IdenitifableModelling;
import eu.amidst.flinklink.core.learning.parametric.utils.ParameterIdentifiableModel;
import eu.amidst.flinklink.core.utils.ConversionToBatches;
import eu.amidst.flinklink.core.utils.Function2;
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
public class dVMP implements BayesianParameterLearningAlgorithm, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    static Logger logger = LoggerFactory.getLogger(dVMP.class);

    public static String PRIOR="PRIOR";
    public static String SVB="SVB";
    public static String LATENT_VARS="LATENT_VARS";


    private static boolean INITIALIZE = false;
    /**
     * Represents the directed acyclic graph {@link DAG}.
     */
    protected DAG dag;

    protected SVB svb;

    protected int batchSize = 100;

    protected int maximumGlobalIterations = 10;

    protected int maximumLocalIterations = 1000;

    protected double globalThreshold = 0.01;

    protected double localThreshold = 0.001;

    protected long timeLimit = -1;

    protected double globalELBO = Double.NaN;

    IdenitifableModelling idenitifableModelling = new ParameterIdentifiableModel();

    boolean randomStart = true;

    Function2<DataFlink<DataInstance>,Integer,DataSet<DataOnMemory<DataInstance>>> batchConverter = ConversionToBatches::toBatches;

    double learningRate = 1.0;

    public dVMP(){
        this.svb = new SVB();
    }


    /**
     * Add a parameter constraint
     * @param constraint, a well defined object constraint.
     */
    public void addParameterConstraint(Constraint constraint){
        this.svb.addParameterConstraint(constraint);
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public void setBatchConverter(Function2<DataFlink<DataInstance>, Integer, DataSet<DataOnMemory<DataInstance>>> batchConverter) {
        this.batchConverter = batchConverter;
    }

    public void setIdenitifableModelling(IdenitifableModelling idenitifableModelling) {
        this.idenitifableModelling = idenitifableModelling;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPlateuStructure(PlateuStructure plateuStructure){
        this.svb.setPlateuStructure(plateuStructure);
    }

    public PlateuStructure getPlateuStructure(){
        return this.svb.getPlateuStructure();
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
        VMPLocalUpdates vmpLocalUpdates = new VMPLocalUpdates(this.svb.getPlateuStructure());
        //VMPParameterv1 vmpLocalUpdates = new VMPParameterv1(this.svb.getPlateuStructure());

        this.svb.getPlateuStructure().setVmp(vmpLocalUpdates);
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

    public DataSet<DataPosteriorAssignment> computePosteriorAssignment(DataFlink<DataInstance> dataFlink, List<Variable> latentVariables){

        Attribute seq_id = dataFlink.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        try{
            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));
            config.setBytes(LATENT_VARS, Serialization.serializeObject(latentVariables));

            return dataFlink
                    .getBatchedDataSet(this.batchSize,batchConverter)
                    .flatMap(new ParallelVBMapInferenceAssignment())
                    .withParameters(config);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }

    public DataSet<DataPosterior> computePosterior(DataFlink<DataInstance> dataFlink, List<Variable> latentVariables){

        Attribute seq_id = dataFlink.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        try{
            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));
            config.setBytes(LATENT_VARS, Serialization.serializeObject(latentVariables));

            return dataFlink
                    .getBatchedDataSet(this.batchSize,batchConverter)
                    .flatMap(new ParallelVBMapInference())
                    .withParameters(config);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }

    public DataSet<DataPosterior> computePosterior(DataFlink<DataInstance> dataFlink){

        Attribute seq_id = dataFlink.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        try{
            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));

            return dataFlink
                    .getBatchedDataSet(this.batchSize,batchConverter)
                    .flatMap(new ParallelVBMapInference())
                    .withParameters(config);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }

    private String getName(){
        if (dag!=null)
            return dag.getName();
        else
            return "";
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
                    .registerAggregationConvergenceCriterion("ELBO_" + this.getName(), new DoubleSumAggregator(),convergenceELBO);

            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));

            //We add an empty batched data set to emit the updated prior.
            DataOnMemory<DataInstance> emtpyBatch = new DataOnMemoryListContainer<DataInstance>(dataUpdate.getAttributes());
            DataSet<DataOnMemory<DataInstance>> unionData = null;

            unionData =
                    dataUpdate.getBatchedDataSet(this.batchSize, batchConverter)
                            .union(env.fromCollection(Arrays.asList(emtpyBatch),
                                    TypeExtractor.getForClass((Class<DataOnMemory<DataInstance>>) Class.forName("eu.amidst.core.datastream.DataOnMemory"))));

            DataSet<CompoundVector> newparamSet =
                    unionData
                    .map(new ParallelVBMap(randomStart, idenitifableModelling))
                    .withParameters(config)
                    .withBroadcastSet(loop, "VB_PARAMS_" + this.getName())
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
            System.out.println(ex.getMessage().toString());
            ex.printStackTrace();
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
        this.svb.setDAG(dag_);
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

    /**
     * {@inheritDoc}
     */
    @Override
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

        double learningRate = 1.0;

        public ParallelVBMap(boolean randomStart, IdenitifableModelling idenitifableModelling) {
            this.randomStart = randomStart;
            this.idenitifableModelling = idenitifableModelling;
        }

        public ParallelVBMap(boolean randomStart, IdenitifableModelling idenitifableModelling, double learningRate) {
            this.randomStart = randomStart;
            this.idenitifableModelling = idenitifableModelling;
            this.learningRate = learningRate;
        }

        @Override
        public CompoundVector map(DataOnMemory<DataInstance> dataBatch) throws Exception {

            if (dataBatch.getNumberOfDataInstances()==0){
                elbo.aggregate(basedELBO);
                System.out.println(basedELBO);
                return prior;//this.svb.getNaturalParameterPrior();
            }else {

                this.svb.updateNaturalParameterPosteriors(updatedPosterior);

/*                svb.getPlateuStructure().getNonReplictedNodes().forEach(node -> node.setActive(false));
                svb.setOutput(true);
                SVB.BatchOutput outElbo = svb.updateModelOnBatchParallel(dataBatch);
                svb.setOutput(true);

                if (Double.isNaN(outElbo.getElbo()))
                    throw new IllegalStateException("NaN elbo");

                elbo.aggregate(outElbo.getElbo());
*/



                //Set Active Parameters
                int superstep = getIterationRuntimeContext().getSuperstepNumber() - 1;
                /*svb.getPlateuStructure()
                        .getNonReplictedNodes()
                        .forEach(node ->
                                node.setActive(this.idenitifableModelling.isActiveAtEpoch(node.getMainVariable(), superstep))
                        );
                */

                if (superstep == 0){
                    this.svb.getPlateuStructure().setSeed(this.svb.getSeed());
                    this.svb.getPlateuStructure().resetQs();
                    this.svb.updateNaturalParameterPosteriors(updatedPosterior);
                }

                SVB.BatchOutput outElbo = svb.updateModelOnBatchParallel(dataBatch);

                elbo.aggregate(outElbo.getElbo());


                //System.out.println(svb.getLearntBayesianNetwork());

                if (Double.isNaN(outElbo.getElbo()))
                    throw new IllegalStateException("NaN elbo");

                return outElbo.getVector();
            }

        }


        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            bnName = parameters.getString(BN_NAME, "");
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));
            int superstep = getIterationRuntimeContext().getSuperstepNumber() - 1;
            if (INITIALIZE && superstep==0) {
                VMP vmp = new VMP();
                vmp.setMaxIter(this.svb.getPlateuStructure().getVMP().getMaxIter());
                vmp.setThreshold(this.svb.getPlateuStructure().getVMP().getThreshold());
                vmp.setTestELBO(this.svb.getPlateuStructure().getVMP().isOutput());
                this.svb.getPlateuStructure().setVmp(vmp);

            }

            if (INITIALIZE && superstep==0 && GlobalvsLocalUpdate.class.isAssignableFrom(this.svb.getPlateuStructure().getClass())){
                ((GlobalvsLocalUpdate)this.svb.getPlateuStructure()).setGlobalUpdate(true);
            }

            if (INITIALIZE && superstep>0 && GlobalvsLocalUpdate.class.isAssignableFrom(this.svb.getPlateuStructure().getClass())){
                ((GlobalvsLocalUpdate)this.svb.getPlateuStructure()).setGlobalUpdate(false);
            }

        svb.initLearning();

            Collection<CompoundVector> collection = getRuntimeContext().getBroadcastVariable("VB_PARAMS_" + bnName);

            if(updatedPosterior==null || learningRate==1.0)
                updatedPosterior = collection.iterator().next();
            else{
                updatedPosterior.multiplyBy(1-learningRate);
                CompoundVector update= collection.iterator().next();
                update.multiplyBy(learningRate);
                updatedPosterior.sum(update);
            }


            if (prior!=null) {
                svb.updateNaturalParameterPrior(prior);
                svb.updateNaturalParameterPosteriors(updatedPosterior);
                basedELBO = svb.getPlateuStructure().getNonReplictedNodes().mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum();
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

            this.svb.setNonSequentialModel(true);

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

/*
            if (iteration==1)
                return false;

            iteration--;
*/
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
//                throw new IllegalStateException("Global bound is not monotonically increasing: "+ iteration +","+
//                        df.format(percentage) +"," + df.format(value.getValue()) +" < " + df.format(previousELBO));
                System.out.println("Global bound is not monotonically increasing: "+ iteration +", "+ percentage +
                 ", "+ (value.getValue() +">" + previousELBO));
                this.previousELBO=value.getValue();
                return false;
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

/*
            if (iteration==1)
                return false;

            iteration--;
*/
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
                //throw new IllegalStateException("Global bound is not monotonically increasing: "+ iteration +","+
                //        df.format(percentage) +"," + df.format(value.getValue()) +" < " + df.format(previousELBO));

                System.out.println("Global bound is not monotonically increasing: "+ iteration +","+percentage+
                        "," + (value.getValue()) +">" + previousELBO+ ","+
                        (System.nanoTime() - start) / 1000000000.0 + " seconds");
                this.previousELBO=value.getValue();
                return false;
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