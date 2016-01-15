/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.learning.dynamic;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;

import java.util.List;

/**
 * This class implements the {@link DynamicBayesianLearningAlgorithm } interface.
 * It defines the Dynamic Streaming Variational Bayes (SVB) algorithm.
 *
 * TODO: By iterating several times over the data we can get better approximations.
 * TODO: Trick. Initialize the Q's of the parameters variables with the final posterios in the previous iterations.
 */
public class DynamicSVB implements DynamicBayesianLearningAlgorithm {

    /** Represents an {@link EF_LearningBayesianNetwork} object at time 0. */
    EF_LearningBayesianNetwork ef_extendedBNTime0;

    /** Represents an {@link EF_LearningBayesianNetwork} object at time 0. */
    EF_LearningBayesianNetwork ef_extendedBNTimeT;

    /** Represents the plateau structure {@link DynamicPlateauStructure}*/
    DynamicPlateauStructure dynamicPlateauStructure = new DynamicPlateauStructure();

    /** Represents a dynamic directed acyclic graph {@link DynamicDAG}. */
    DynamicDAG dag;

    /** Represents the data stream to be used for parameter learning. */
    DataStream<DynamicDataInstance> dataStream;

    /** Represents the Evidence Lower BOund (elbo). */
    double elbo;

    /** Indicates the parallel processing mode, initialized to {@code false}. */
    boolean parallelMode=false;

    /** Represents the window size, initialized to 100. */
    int windowsSize=100;

    /** Represents the seed, initialized to 0. */
    int seed = 0;

    /**
     * Returns the dynamic plateu structure of this DynamicSVB.
     * @return a {@link DynamicPlateauStructure} object.
     */
    public DynamicPlateauStructure getDynamicPlateauStructure() {
        return dynamicPlateauStructure;
    }

    /**
     * Creates a new DynamicSVB.
     */
    public DynamicSVB(){
        dynamicPlateauStructure = new DynamicPlateauStructure();
        dynamicPlateauStructure.setNRepetitions(windowsSize);
    }

    /**
     * Returns the seed value.
     */
    public int getSeed() {
        return seed;
    }

    /**
     * Sets the seed.
     * @param seed a given {@code int} seed value.
     */
    public void setSeed(int seed) {
        this.seed = seed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogMarginalProbability() {
        return elbo;
    }

    /**
     * Sets the window size.
     * @param windowsSize the window size.
     */
    public void setWindowsSize(int windowsSize) {
        this.windowsSize = windowsSize;
        this.dynamicPlateauStructure.setNRepetitions(windowsSize);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {
        this.initLearning();
        if (!parallelMode) {
            //this.elbo = this.dataStream.stream().sequential().mapToDouble(this::updateModel).sumNonStateless();
            this.elbo = this.dataStream.streamOfBatches(this.windowsSize).sequential().mapToDouble(this::updateModel).sum();
        }else {
            //Create EF_ExtendedBN which returns ParameterVariable object
            //Paremter variable car
            //BatchOutput finalout = this.dataStream.streamOfBatches(100).map(this::updateModelOnBatchParallel).reduce(BatchOutput::sumNonStateless).get();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataOnMemory<DynamicDataInstance> batch) {

        List<DynamicDataInstance> data = batch.getList();
        double logprob = 0;
        if (batch.getDataInstance(0).getTimeID()==0){
            logprob+=this.updateModelTime0(batch.getDataInstance(0));
            data.remove(0);
            if (data.size()==0)
                return logprob;

        }
        logprob+=this.updateModelTimeT(data);
        return logprob;
    }

    /**
     * Updates the model at time 0 using a given {@link DynamicDataInstance}.
     * @param dataInstance a {@link DynamicDataInstance} object.
     * @return a {@code double} value.
     */
    private double updateModelTime0(DynamicDataInstance dataInstance) {
        this.dynamicPlateauStructure.setEvidenceTime0(dataInstance);
        this.dynamicPlateauStructure.runInferenceTime0();

        for (Variable var: dynamicPlateauStructure.getEFLearningBNTime0().getParametersVariables()){
            EF_UnivariateDistribution uni = dynamicPlateauStructure.getEFParameterPosteriorTime0(var).deepCopy();
            dynamicPlateauStructure.getEFLearningBNTime0().setDistribution(var, uni);
            this.dynamicPlateauStructure.getNodeOfVarTime0(var).setPDist(uni);
        }
        return this.dynamicPlateauStructure.getLogProbabilityOfEvidenceTime0();
    }

    /**
     * Updates the model at time T using a given list of {@link DynamicDataInstance}s.
     * @param batch a {@code List} of {@link DynamicDataInstance}s.
     * @return a {@code double} value.
     */
    private double updateModelTimeT(List<DynamicDataInstance> batch) {
        this.dynamicPlateauStructure.setEvidenceTimeT(batch);
        this.dynamicPlateauStructure.runInferenceTimeT();

        for (Variable var: dynamicPlateauStructure.getEFLearningBNTimeT().getParametersVariables()){
            EF_UnivariateDistribution uni = dynamicPlateauStructure.getEFParameterPosteriorTimeT(var).deepCopy();
            dynamicPlateauStructure.getEFLearningBNTimeT().setDistribution(var,uni);
            this.dynamicPlateauStructure.getNodeOfVarTimeT(var,0).setPDist(uni);
        }
        return this.dynamicPlateauStructure.getLogProbabilityOfEvidenceTimeT();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDynamicDAG(DynamicDAG dag) {
        this.dag = dag;
    }

    public void initLearning(){
        this.dynamicPlateauStructure.setSeed(seed);
        this.dynamicPlateauStructure.setDBNModel(this.dag);
        this.dynamicPlateauStructure.resetQs();
        this.ef_extendedBNTime0 = this.dynamicPlateauStructure.getEFLearningBNTime0();
        this.ef_extendedBNTimeT = this.dynamicPlateauStructure.getEFLearningBNTimeT();
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
    public DynamicBayesianNetwork getLearntDBN() {
        return new DynamicBayesianNetwork(this.dag, this.ef_extendedBNTime0.toConditionalDistribution(), this.ef_extendedBNTimeT.toConditionalDistribution());
    }

}
