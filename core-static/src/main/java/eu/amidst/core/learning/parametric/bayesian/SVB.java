/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.learning.parametric.bayesian;

import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Vector;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * TODO By iterating several times over the data we can get better approximations. Trick. Initialize the Q's of the parameters variables with the final posterios in the previous iterations.
 *
 *
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class SVB implements BayesianParameterLearningAlgorithm {

    TransitionMethod transitionMethod = null;
    EF_LearningBayesianNetwork ef_extendedBN;
    PlateuStructure plateuStructure = new PlateuIIDReplication();
    DAG dag;
    DataStream<DataInstance> dataStream;
    double elbo;
    boolean nonSequentialModel=false;
    boolean randomRestart=false;
    int windowsSize=100;
    int seed = 0;
    int nBatches = 0;
    int nIterTotal = 0;

    CompoundVector naturalVectorPrior = null;

    BatchOutput naturalVectorPosterior = null;


    public int getWindowsSize() {
        return windowsSize;
    }

    public void setRandomRestart(boolean randomRestart) {
        this.randomRestart = randomRestart;
    }

    public PlateuStructure getPlateuStructure() {
        return plateuStructure;
    }

    public void setPlateuStructure(PlateuStructure plateuStructure) {
        this.plateuStructure = plateuStructure;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    @Override
    public double getLogMarginalProbability() {
        return elbo;
    }

    public void setWindowsSize(int windowsSize) {
        this.windowsSize = windowsSize;
    }

    public void setTransitionMethod(TransitionMethod transitionMethod) {
        this.transitionMethod = transitionMethod;
    }

    public <E extends TransitionMethod> E getTransitionMethod() {
        return (E)this.transitionMethod;
    }


    protected CompoundVector getNaturalParameterPrior(){
        if (naturalVectorPrior==null){
            naturalVectorPrior = this.computeNaturalParameterVectorPrior();
        }

        return naturalVectorPrior;
    }

    protected BatchOutput getNaturalParameterPosterior() {
        if (this.naturalVectorPosterior==null){
            naturalVectorPosterior = new BatchOutput(this.computeNaturalParameterVectorPrior(), 0);
        }
        return naturalVectorPosterior;
    }

    protected CompoundVector computeNaturalParameterVectorPrior(){
        List<Vector> naturalParametersPriors =  this.ef_extendedBN.getParametersVariables().getListOfVariables().stream()
                .filter( var -> this.getPlateuStructure().getNodeOfVar(var,0).isActive())
                .map(var -> {
                    NaturalParameters parameter =this.ef_extendedBN.getDistribution(var).getNaturalParameters();
                    NaturalParameters copy = new ArrayVector(parameter.size());
                    copy.copy(parameter);
                    return copy;
                }).collect(Collectors.toList());

        CompoundVector compoundVectorPrior = new CompoundVector(naturalParametersPriors);

        return compoundVectorPrior;
    }

    @Override
    public void runLearning() {
        this.initLearning();
        if (!nonSequentialModel) {
            this.elbo = this.dataStream.streamOfBatches(this.windowsSize).mapToDouble(this::updateModel).sum();
        }else {
            this.elbo = this.dataStream.streamOfBatches(this.windowsSize).mapToDouble(this::updateModelParallel).sum();
       }
    }

    public void setNonSequentialModel(boolean nonSequentialModel_) {
        this.nonSequentialModel = nonSequentialModel_;
    }

    @Override
    public void setOutput(boolean activateOutput) {
        this.getPlateuStructure().getVMP().setOutput(activateOutput);
    }

    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {
        double elboBatch = 0;
        if (!nonSequentialModel){
            if (this.randomRestart) this.getPlateuStructure().resetQs();
            elboBatch =  this.updateModelSequential(batch);
        }else{
            if (this.randomRestart) this.getPlateuStructure().resetQs();
            elboBatch =  this.updateModelParallel(batch);
        }

        if (transitionMethod!=null)
            this.ef_extendedBN=this.transitionMethod.transitionModel(this.ef_extendedBN, this.plateuStructure);

        return elboBatch;
    }

    private double updateModelSequential(DataOnMemory<DataInstance> batch) {
        nBatches++;
        //System.out.println("\n Batch:");
        this.plateuStructure.setEvidence(batch.getList());
        this.plateuStructure.runInference();
        nIterTotal+=this.plateuStructure.getVMP().getNumberOfIterations();

        ef_extendedBN.getParametersVariables().getListOfVariables().stream().forEach(var -> {
            EF_UnivariateDistribution uni = plateuStructure.getEFParameterPosterior(var).deepCopy();
            ef_extendedBN.setDistribution(var, uni);
            this.plateuStructure.getNodeOfVar(var,0).setPDist(uni);
        });

        //this.plateuVMP.resetQs();
        return this.plateuStructure.getLogProbabilityOfEvidence();
    }

    protected BatchOutput updateModelOnBatchParallel(DataOnMemory<DataInstance> batch) {

        nBatches++;
        this.plateuStructure.setEvidence(batch.getList());
        this.plateuStructure.runInference();
        nIterTotal+=this.plateuStructure.getVMP().getNumberOfIterations();

        List<Vector> naturalParametersPosterior =  this.ef_extendedBN.getParametersVariables().getListOfVariables().stream()
                .filter(var -> this.getPlateuStructure().getNodeOfVar(var, 0).isActive())
                .map(var -> plateuStructure.getEFParameterPosterior(var).deepCopy().getNaturalParameters()).collect(Collectors.toList());


        CompoundVector compoundVectorEnd = new CompoundVector(naturalParametersPosterior);

        compoundVectorEnd.substract(this.getNaturalParameterPrior());

        return new BatchOutput(compoundVectorEnd, this.plateuStructure.getLogProbabilityOfEvidence());

    }

    private double updateModelParallel(DataOnMemory<DataInstance> batch) {

        nBatches++;
        this.plateuStructure.setEvidence(batch.getList());
        this.plateuStructure.runInference();
        nIterTotal+=this.plateuStructure.getVMP().getNumberOfIterations();

        List<Vector> naturalParametersPosterior =  this.ef_extendedBN.getParametersVariables().getListOfVariables().stream()
                .filter( var -> this.getPlateuStructure().getNodeOfVar(var,0).isActive())
                .map(var -> plateuStructure.getEFParameterPosterior(var).deepCopy().getNaturalParameters()).collect(Collectors.toList());


        CompoundVector compoundVectorEnd = new CompoundVector(naturalParametersPosterior);

        compoundVectorEnd.substract(this.getNaturalParameterPrior());

        BatchOutput out = new BatchOutput(compoundVectorEnd, this.plateuStructure.getLogProbabilityOfEvidence());

        this.naturalVectorPosterior = BatchOutput.sum(out, this.getNaturalParameterPosterior());

        return out.getElbo();
    }

    public int getNumberOfBatches() {
        return nBatches;
    }

    public double getAverageNumOfIterations(){
        return ((double)nIterTotal)/nBatches;
    }

    @Override
    public void setDAG(DAG dag) {
        this.dag = dag;
    }

    public void initLearning(){

        plateuStructure.setNRepetitions(windowsSize);

        this.nBatches = 0;
        this.nIterTotal = 0;
        List<EF_ConditionalDistribution> dists = this.dag.getParentSets().stream()
                .map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents()))
                .collect(Collectors.toList());

        this.ef_extendedBN = new EF_LearningBayesianNetwork(dists);
        this.plateuStructure.setSeed(seed);
        plateuStructure.setEFBayesianNetwork(ef_extendedBN);
        plateuStructure.replicateModel();
        this.plateuStructure.resetQs();
        if (transitionMethod!=null)
           this.ef_extendedBN = this.transitionMethod.initModel(this.ef_extendedBN, plateuStructure);
    }

    @Override
    public void setDataStream(DataStream<DataInstance> data) {
        this.dataStream=data;
    }

    private static EF_BayesianNetwork convertDAGToExtendedEFBN(DAG dag){
        return null;
    }

    protected void updateNaturalParameterPrior(CompoundVector parameterVector){
        final int[] count = new int[1];
        ef_extendedBN.getParametersVariables().getListOfVariables().stream()
                .filter( var -> this.getPlateuStructure().getNodeOfVar(var,0).isActive())
                .forEach( var -> {
                    EF_UnivariateDistribution uni = plateuStructure.getEFParameterPosterior(var).deepCopy();
                    uni.getNaturalParameters().copy(parameterVector.getVectorByPosition(count[0]));
                    uni.updateMomentFromNaturalParameters();
                    ef_extendedBN.setDistribution(var, uni);
                    this.plateuStructure.getNodeOfVar(var,0).setPDist(uni);
                    count[0]++;
                });
    }

    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        if(!nonSequentialModel)
            return BayesianNetwork.newBayesianNetwork(this.dag, ef_extendedBN.toConditionalDistribution());
        else{
            CompoundVector posterior = (CompoundVector)this.getNaturalParameterPosterior().getVector();

            final int[] count = new int[1];
            ef_extendedBN.getParametersVariables().getListOfVariables().stream()
                    .filter( var -> this.getPlateuStructure().getNodeOfVar(var,0).isActive())
                    .forEach( var -> {
                        EF_UnivariateDistribution uni = plateuStructure.getEFParameterPosterior(var).deepCopy();
                        uni.getNaturalParameters().copy(posterior.getVectorByPosition(count[0]));
                        uni.updateMomentFromNaturalParameters();
                        ef_extendedBN.setDistribution(var, uni);
                        count[0]++;
            });

            BayesianNetwork learntBN =  BayesianNetwork.newBayesianNetwork(this.dag, ef_extendedBN.toConditionalDistribution());


            CompoundVector priors = this.getNaturalParameterPrior();
            count[0] = 0;
            ef_extendedBN.getParametersVariables().getListOfVariables().stream()
                    .filter( var -> this.getPlateuStructure().getNodeOfVar(var,0).isActive())
                    .forEach( var -> {
                        EF_UnivariateDistribution uni = plateuStructure.getEFParameterPosterior(var).deepCopy();
                        uni.getNaturalParameters().copy(priors.getVectorByPosition(count[0]));
                        uni.updateMomentFromNaturalParameters();
                        ef_extendedBN.setDistribution(var, uni);
                        count[0]++;
                    });


            return learntBN;
        }
    }

    @Override
    public void setParallelMode(boolean parallelMode) {
        throw new UnsupportedOperationException("Non Parallel Mode Supported. Use class ParallelSVB");
    }

    static class BatchOutput{
        CompoundVector vector;
        double elbo;

        BatchOutput(CompoundVector vector_, double elbo_) {
            this.vector = vector_;
            this.elbo = elbo_;
        }

        public CompoundVector getVector() {
            return vector;
        }

        public double getElbo() {
            return elbo;
        }

        public void setElbo(double elbo) {
            this.elbo = elbo;
        }

        public static BatchOutput sum(BatchOutput batchOutput1, BatchOutput batchOutput2){
            batchOutput2.getVector().sum(batchOutput1.getVector());
            batchOutput2.setElbo(batchOutput2.getElbo()+batchOutput1.getElbo());
            return batchOutput2;
        }
    }



}
