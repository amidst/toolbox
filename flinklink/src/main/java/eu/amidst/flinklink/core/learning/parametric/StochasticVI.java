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


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.learning.parametric.bayesian.utils.TransitionMethod;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.utils.Function2;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * This class implements the {@link ParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class StochasticVI implements BayesianParameterLearningAlgorithm, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    static Logger logger = LoggerFactory.getLogger(ParallelVB.class);

    public static String SVB="SVB";
    public static String PRIOR="PRIOR";

    /**
     * Represents the directed acyclic graph {@link DAG}.
     */
    protected eu.amidst.core.learning.parametric.bayesian.StochasticVI svi;

    private long timiLimit;

    Function2<DataFlink<DataInstance>,Integer,DataSet<DataOnMemory<DataInstance>>> batchConverter=null;


    public void setBatchConverter(Function2<DataFlink<DataInstance>, Integer, DataSet<DataOnMemory<DataInstance>>> batchConverter) {
        this.batchConverter = batchConverter;
    }
    public void setLearningFactor(double learningFactor) {
        this.svi.setLearningFactor(learningFactor);
    }

    public void setTimiLimit(long seconds) {
        this.timiLimit = seconds;
    }

    public void setDataSetSize(int dataSetSize) {
        this.svi.setDataSetSize(dataSetSize);
    }

    public StochasticVI(){
        this.svi = new eu.amidst.core.learning.parametric.bayesian.StochasticVI();
    }

    public void setPlateuStructure(PlateuStructure plateuStructure){
        this.svi.setPlateuStructure(plateuStructure);
    }

    public void setTransitionMethod(TransitionMethod transitionMethod){
        this.svi.setTransitionMethod(transitionMethod);
    }

    public void setLocalThreshold(double localThreshold) {
         this.svi.setLocalThreshold(localThreshold);
    }

    public void setMaximumLocalIterations(int maximumLocalIterations) {
        this.svi.setMaximumLocalIterations(maximumLocalIterations);
    }

    @Override
    public void setBatchSize(int batchSize) {
        this.svi.setBatchSize(batchSize);
    }

    public eu.amidst.core.learning.parametric.bayesian.StochasticVI getSVI() {
        return this.svi;
    }

    public void initLearning() {
        this.svi.initLearning();
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
    public double updateModel(DataFlink<DataInstance> dataUpdate) {

        DecimalFormat df = new DecimalFormat("0.0000");

        boolean convergence=false;

        double totalTimeElbo=0;

        double totalTime=0;
        double t = 0;
        while(!convergence){

            long startBatch= System.nanoTime();

            DataOnMemory<DataInstance> batch;

            if (batchConverter==null)
                    batch= dataUpdate.subsample(this.svi.getSeed(), this.svi.getBatchSize());
            else
                    batch= dataUpdate.subsample(this.svi.getSeed(), this.svi.getBatchSize(), this.batchConverter);

            this.svi.updateModel(batch);

            long endBatch= System.nanoTime();

            totalTime+=endBatch-startBatch;

            if (t%10==0) {
                long startBatchELBO = System.nanoTime();
                //Compute ELBO

                double elbo = this.computeELBO(dataUpdate, svi.getSVB(), this.batchConverter);

                long endBatchELBO = System.nanoTime();

                totalTimeElbo += endBatchELBO - startBatchELBO;

                System.out.println("TIME ELBO:" + totalTimeElbo / 1e9);


                logger.info("SVI ELBO: {},{},{},{} seconds, {} seconds", t, 0,
                        df.format(elbo), df.format(totalTime / 1e9), df.format(totalTimeElbo / 1e9));

                System.out.println("SVI ELBO: " + t + ", " + 0.0 + ", " + elbo + ", " + totalTime / 1e9 + " seconds " + totalTimeElbo / 1e9 + " seconds " + (totalTime - totalTimeElbo) / 1e9 + " seconds");
            }

            if ((totalTime-totalTimeElbo)/1e9>timiLimit){// || t>this.maximumLocalIterations){
                convergence=true;
            }

            t++;

        }

        return this.getLogMarginalProbability();

    }
    public static double computeELBO(DataFlink<DataInstance> dataFlink, SVB svb) {
            return computeELBO(dataFlink,svb,null);
    }

    public static double computeELBO(DataFlink<DataInstance> dataFlink, SVB svb, Function2<DataFlink<DataInstance>,Integer,DataSet<DataOnMemory<DataInstance>>> batchConverter){

        svb.setOutput(false);
        double elbo =  svb.getPlateuStructure().getNonReplictedNodes().mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum();

        try {

            Configuration config = new Configuration();
            config.setBytes(SVB, Serialization.serializeObject(svb));
            config.setBytes(PRIOR, Serialization.serializeObject(svb.getPlateuStructure().getPlateauNaturalParameterPosterior()));

            DataSet<DataOnMemory<DataInstance>> batches;
            if (batchConverter!=null)
                batches= dataFlink.getBatchedDataSet(svb.getWindowsSize(),batchConverter);
            else
                batches= dataFlink.getBatchedDataSet(svb.getWindowsSize());

            elbo += batches.map(new ParallelVBMapELBO())
                    .withParameters(config)
                    .reduce(new ReduceFunction<Double>() {
                        @Override
                        public Double reduce(Double aDouble, Double t1) throws Exception {
                            return aDouble + t1;
                        }
                    }).collect().get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        svb.setOutput(true);

        return elbo;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setDAG(DAG dag_) {
        this.svi.setDAG(dag_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSeed(int seed) {
        this.svi.setSeed(seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        return this.svi.getLearntBayesianNetwork();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutput(boolean activateOutput) {
        this.svi.setOutput(activateOutput);
    }


    public <E extends UnivariateDistribution> E getParameterPosterior(Variable parameter) {
        return this.svi.getParameterPosterior(parameter);
    }


    public static class ParallelVBMapELBO extends RichMapFunction<DataOnMemory<DataInstance>, Double> {

        SVB svb;
        CompoundVector prior;

        @Override
        public Double map(DataOnMemory<DataInstance> dataBatch) throws Exception {
                //Compute ELBO
                this.svb.setOutput(false);
                SVB.BatchOutput outElbo = svb.updateModelOnBatchParallel(dataBatch);

                if (Double.isNaN(outElbo.getElbo()))
                    throw new IllegalStateException("NaN elbo");

                return outElbo.getElbo();
        }


        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));
            this.prior = Serialization.deserializeObject(parameters.getBytes(PRIOR, null));

            svb.initLearning();
            this.svb.updateNaturalParameterPrior(prior);
            this.svb.updateNaturalParameterPosteriors(prior);

            svb.getPlateuStructure().getNonReplictedNodes().forEach(node -> node.setActive(false));

        }

    }

}