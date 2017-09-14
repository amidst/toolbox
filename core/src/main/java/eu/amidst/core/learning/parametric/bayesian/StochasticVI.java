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
package eu.amidst.core.learning.parametric.bayesian;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.learning.parametric.bayesian.utils.DataPosterior;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.learning.parametric.bayesian.utils.TransitionMethod;
import eu.amidst.core.learning.parametric.bayesian.utils.VMPLocalUpdates;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * This class implements the {@link BayesianParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class StochasticVI implements BayesianParameterLearningAlgorithm, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    public static String SVB="SVB";
    public static String PRIOR="PRIOR";


    /**
     * Represents the {@link DataStream} used for learning the parameters.
     */
    protected DataStream<DataInstance> dataStream;

    /**
     * Represents the directed acyclic graph {@link DAG}.
     */
    protected DAG dag;

    protected SVB svb;

    protected int batchSize = 100;

    protected int maximumLocalIterations = 100;

    protected double localThreshold = 0.1;

    protected long dataSetSize;

    private long timiLimit;

    private double learningFactor=0.75;
    private CompoundVector prior;
    private CompoundVector initialPosterior;
    private CompoundVector currentParam;
    private int iteration;
    private boolean fixedStepSize =false;


    public void setFixedStepSize(boolean fixedStepSize) {
        this.fixedStepSize = fixedStepSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getSeed(){
        return this.svb.getSeed();
    }

    public void setLearningFactor(double learningFactor) {
        this.learningFactor = learningFactor;
    }

    public void setTimiLimit(long seconds) {
        this.timiLimit = seconds;
    }

    public void setDataSetSize(int dataSetSize) {
        this.dataSetSize = dataSetSize;
    }

    public StochasticVI(){
        this.svb = new SVB();
        this.svb.setNonSequentialModel(true);
    }

    public void setPlateuStructure(PlateuStructure plateuStructure){
        this.svb.setPlateuStructure(plateuStructure);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialize() {
        this.svb.randomInitialize();
    }

    public void setTransitionMethod(TransitionMethod transitionMethod){
        this.svb.setTransitionMethod(transitionMethod);
    }

    public void setLocalThreshold(double localThreshold) {
        this.localThreshold = localThreshold;
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

        //TODO: Remove the code inside this method once issue #50 is solved.
        VMPLocalUpdates vmpLocalUpdates = new VMPLocalUpdates(this.svb.getPlateuStructure());
        this.svb.getPlateuStructure().setVmp(vmpLocalUpdates);
        this.svb.getPlateuStructure().getVMP().setMaxIter(this.maximumLocalIterations);
        this.svb.getPlateuStructure().getVMP().setThreshold(this.localThreshold);
        this.svb.setDAG(this.dag);
        this.svb.setWindowsSize(batchSize);
        this.svb.initLearning(); //Init learning is peformed in each mapper.



        prior = svb.getNaturalParameterPrior();

        initialPosterior = Serialization.deepCopy(this.svb.getPlateuStructure().getPlateauNaturalParameterPosterior());
        initialPosterior.sum(prior);

        this.svb.updateNaturalParameterPosteriors(initialPosterior);

        currentParam =  svb.getNaturalParameterPrior();

        iteration=0;

    }

    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {

        NaturalParameters newParam = svb.updateModelOnBatchParallel(batch).getVector();

        newParam.multiplyBy(this.dataSetSize/(double)batch.getNumberOfDataInstances());
        newParam.sum(prior);

        double stepSize = 0;
        if (this.fixedStepSize){
            stepSize = learningFactor;
        }else {
            stepSize = Math.pow(1 + iteration, -learningFactor);
        }
        newParam.multiplyBy(stepSize);

        currentParam.multiplyBy((1-stepSize));
        currentParam.sum(newParam);

        this.svb.updateNaturalParameterPosteriors(currentParam);

        iteration++;

        return Double.NaN;
    }

    @Override
    public int getWindowsSize() {
        throw new UnsupportedOperationException("Use method getBatchSise() instead.");
    }

    @Override
    public void setWindowsSize(int windowsSize) {
        throw new UnsupportedOperationException("Use method setBatchSise() instead.");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataStream(DataStream<DataInstance> data) {
        this.dataStream = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogMarginalProbability() {
        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {
        this.initLearning();

        boolean convergence=false;


        double totalTimeElbo=0;

        double totalTime=0;

        Iterator<DataOnMemory<DataInstance>> iterator = this.dataStream.iterableOverBatches(this.batchSize).iterator();



        while(!convergence){

            long startBatch= System.nanoTime();

            DataOnMemory<DataInstance> batch = iterator.next();

            if (!iterator.hasNext())
                iterator = this.dataStream.iterableOverBatches(this.batchSize).iterator();

            NaturalParameters newParam = svb.updateModelOnBatchParallel(batch).getVector();

            newParam.multiplyBy(this.dataSetSize/(double)this.batchSize);
            newParam.sum(prior);

            double stepSize = Math.pow(1+ iteration,-learningFactor);

            newParam.multiplyBy(stepSize);

            currentParam.multiplyBy((1-stepSize));
            currentParam.sum(newParam);

            this.svb.updateNaturalParameterPosteriors(currentParam);




            long startBatchELBO= System.nanoTime();

            long endBatch= System.nanoTime();

            totalTimeElbo += endBatch - startBatchELBO;

            System.out.println("TIME ELBO:" + totalTimeElbo/1e9);

            totalTime+=endBatch-startBatch;


            System.out.println("SVI ELBO: "+ iteration +", "+stepSize+", "+totalTime/1e9+" seconds "+ totalTimeElbo/1e9 + " seconds" + (totalTime - totalTimeElbo)/1e9 + " seconds");


            if ((totalTime-totalTimeElbo)/1e9>timiLimit || iteration>this.maximumLocalIterations){
                convergence=true;
            }

            iteration++;

        }
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

    @Override
    public void setParallelMode(boolean parallelMode) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutput(boolean activateOutput) {
        this.svb.setOutput(activateOutput);
    }


    @Override
    public List<DataPosterior> computePosterior(DataOnMemory<DataInstance> batch) {
        return null;
    }

    @Override
    public List<DataPosterior> computePosterior(DataOnMemory<DataInstance> batch, List<Variable> latentVariables) {
        return null;
    }

    public <E extends UnivariateDistribution> E getParameterPosterior(Variable parameter) {
        return this.svb.getParameterPosterior(parameter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double predictedLogLikelihood(DataOnMemory<DataInstance> batch) {
        return this.svb.predictedLogLikelihood(batch);
    }

}