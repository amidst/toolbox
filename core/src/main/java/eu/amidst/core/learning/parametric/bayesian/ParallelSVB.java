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

package eu.amidst.core.learning.parametric.bayesian;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.learning.parametric.bayesian.utils.DataPosterior;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * This class implements the {@link BayesianParameterLearningAlgorithm} interface.
 * It defines the parallel implementation of the Streaming Variational Bayes (SVB) algorithm.
 *
 * <p> For an example of use follow this link
 * <a href="http://amidst.github.io/toolbox/CodeExamples.html#psvbexample"> http://amidst.github.io/toolbox/CodeExamples.html#psvbexample </a>  </p>
 */
public class ParallelSVB implements BayesianParameterLearningAlgorithm{

    /** Represents the data stream to be used for parameter learning. */
    DataStream<DataInstance> data;

    /** Represents the set of SVB engines. */
    SVB[] svbEngines;

    /** Represents a directed acyclic graph {@link DAG}. */
    DAG dag;

    /** Represents the number of used CPU cores. */
    int nCores = -1;

    /** Represents a {@link SVB} object. */
    SVB SVBEngine = new SVB();

    /** Represents the log likelihood. */
    double logLikelihood;

    /** Represents the seed, initialized to 0. */
    int seed = 0;

    /** Indicates if the Output is activated or not, initialized to {@code false}. */
    boolean activateOutput=false;

    /**
     * Sets the seed using a single {@code int} seed.
     * @param seed_ the initial seed.
     */
    public void setSeed(int seed_){
        seed = seed_;
    }

    /**
     * Sets the number of CPU cores.
     * @param nCores the number of CPU cores.
     */
    public void setNCores(int nCores) {
        this.nCores = nCores;
    }

    /**
     * Returns the SVB engine.
     * @return the SVB engine.
     */
    public SVB getSVBEngine() {
        return SVBEngine;
    }

    /**
     * Sets the SVB engine.
     * @param SVBEngine the SVB engine.
     */
    public void setSVBEngine(SVB SVBEngine) {
        this.SVBEngine = SVBEngine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initLearning() {
        if (this.nCores==-1)
            this.nCores=Runtime.getRuntime().availableProcessors();

        this.SVBEngine.setDAG(this.dag);
        this.SVBEngine.setSeed(this.seed);
        this.SVBEngine.initLearning();

        svbEngines = new SVB[nCores];

        for (int i = 0; i < nCores; i++) {
            svbEngines[i] = Serialization.deepCopy(this.SVBEngine);
            svbEngines[i].initLearning();
        }

        this.SVBEngine=svbEngines[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {
        throw new UnsupportedOperationException("Use standard StreamingSVB for sequential updating");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataStream(DataStream<DataInstance> data_) {
        this.data=data_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogMarginalProbability() {
        return this.logLikelihood;
    }

    /**
     * Sets the windows size.
     * @param batchSize_ the batch size.
     */
    public void setWindowsSize(int batchSize_) {
        this.SVBEngine.setWindowsSize(batchSize_);
    }

    /**
     * Sets the windows size.
     */
    public int getWindowsSize() {
        return this.SVBEngine.getWindowsSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {
        this.initLearning();


        Iterator<DataOnMemory<DataInstance>> iterator = this.data.iterableOverBatches(this.SVBEngine.getWindowsSize()).iterator();

        logLikelihood = 0;
        while(iterator.hasNext()){
            CompoundVector posterior =  this.svbEngines[0].getNaturalParameterPrior();

            //Load Data
            List<DataOnMemory<DataInstance>> dataBatches = new ArrayList();
            int cont=0;
            while (iterator.hasNext() && cont<nCores){
                dataBatches.add(iterator.next());
                cont++;
            }

            //Run Inference
            SVB.BatchOutput out=
                    IntStream.range(0, dataBatches.size())
                        .parallel()
                        .mapToObj(i -> this.svbEngines[i].updateModelOnBatchParallel(dataBatches.get(i)))
                        .reduce(SVB.BatchOutput::sumNonStateless)
                        .get();

            //Update logLikelihood
            this.logLikelihood+=out.getElbo();

            //Combine the output
            posterior.sum(out.getVector());
            for (int i = 0; i < nCores; i++) {
                this.svbEngines[i].updateNaturalParameterPrior(posterior);
            }
        }

    }

    /**
     * Update the model in parallel using the provide data stream.
     * @param data, A {@link DataStream} object.
     */
    @Override
    public double updateModel(DataStream<DataInstance> data) {

        logLikelihood = Double.NEGATIVE_INFINITY;
        boolean convergence = false;
        while (!convergence) {
            CompoundVector posterior = this.svbEngines[0].getNaturalParameterPrior();

            Iterator<DataOnMemory<DataInstance>> iterator = data.iterableOverBatches(this.SVBEngine.getWindowsSize()).iterator();
            double local_loglikelihood = 0;
            while (iterator.hasNext()) {

                //Load Data
                List<DataOnMemory<DataInstance>> dataBatches = new ArrayList();
                int cont = 0;
                while (iterator.hasNext() && cont < nCores) {
                    dataBatches.add(iterator.next());
                    cont++;
                }

                //Run Inference
                SVB.BatchOutput out =
                        IntStream.range(0, dataBatches.size())
                                .parallel()
                                .mapToObj(i -> this.svbEngines[i].updateModelOnBatchParallel(dataBatches.get(i)))
                                .reduce(SVB.BatchOutput::sumNonStateless)
                                .get();

                //Combine the output
                posterior.sum(out.getVector());
                local_loglikelihood += out.getElbo();
            }

            for (int i = 0; i < nCores; i++) {
                this.svbEngines[i].updateNaturalParameterPrior(posterior);
            }



            if (Math.abs(logLikelihood - local_loglikelihood)/this.SVBEngine.getPlateuStructure().getNumberOfReplications() < 0.01) {
                convergence = true;
            } else if ((logLikelihood-local_loglikelihood)/this.SVBEngine.getPlateuStructure().getNumberOfReplications()>0.01) {
                throw new IllegalStateException("Non increasing log likelihood: " + local_loglikelihood + " , " + logLikelihood);
            }else {
                //Update logLikelihood
                logLikelihood = local_loglikelihood;
            }
        }

        for (SVB svbEngine : svbEngines) {
            svbEngine.applyTransition();
        }

        return logLikelihood;
    }

    @Override
    public List<DataPosterior> computePosterior(DataOnMemory<DataInstance> batch) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataPosterior> computePosterior(DataOnMemory<DataInstance> batch, List<Variable> latentVariables) {
        throw new UnsupportedOperationException("Method not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double predictedLogLikelihood(DataOnMemory<DataInstance> batch) {
        return this.SVBEngine.predictedLogLikelihood(batch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPlateuStructure(PlateuStructure plateuStructure) {
        this.SVBEngine.setPlateuStructure(plateuStructure);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialize() {
        this.getSVBEngine().randomInitialize();
        for (int i = 0; i < this.svbEngines.length; i++) {
            this.svbEngines[i].randomInitialize();
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
    public BayesianNetwork getLearntBayesianNetwork() {
        return this.svbEngines[0].getLearntBayesianNetwork();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParallelMode(boolean parallelMode) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutput(boolean activateOutput_) {
        activateOutput = activateOutput_;
    }
}