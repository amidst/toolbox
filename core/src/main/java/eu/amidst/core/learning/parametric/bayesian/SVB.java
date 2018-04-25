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

import eu.amidst.core.constraints.Constraint;
import eu.amidst.core.constraints.Constraints;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.bayesian.utils.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// TODO By iterating several times over the data we can get better approximations.
// TODO Trick. Initialize the Q's of the parameters variables with the final posteriors in the previous iterations.

/**
 * This class implements the {@link BayesianParameterLearningAlgorithm} interface.
 * It defines the Streaming Variational Bayes (SVB) algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#svbexample"> http://amidst.github.io/toolbox/CodeExamples.html#svbexample </a>  </p>
 *
 */
public class SVB implements BayesianParameterLearningAlgorithm, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    /** Represents the transition method {@link TransitionMethod}. */
    TransitionMethod transitionMethod = null;

    /** Represents a {@link EF_LearningBayesianNetwork} object. */
    protected EF_LearningBayesianNetwork ef_extendedBN;

    /** Represents the plateu structure {@link PlateuStructure}*/
    protected PlateuStructure plateuStructure = new PlateuIIDReplication();

    /** Represents a directed acyclic graph {@link DAG}. */
    protected DAG dag;

    /** Represents the data stream to be used for parameter learning. */
    transient DataStream<DataInstance> dataStream;

    /** Represents the Evidence Lower BOund (elbo). */
    double elbo;

    /** Indicates if the model is non sequential, initialized to {@code false}. */
    boolean nonSequentialModel=false;

    /** Indicates if this SVB can random restarted, initialized to {@code false}. */
    boolean randomRestart=false;

    /** Represents the window size, initialized to 100. */
    int windowsSize=100;

    /** Represents the seed, initialized to 0. */
    int seed = 0;

    /** Represents the total number of batches. */
    int nBatches = 0;

    /** Represents the total number of iterations, initialized to 0. */
    int nIterTotal = 0;

    /** Represents the natural vector prior. */
    CompoundVector naturalVectorPrior = null;

    /** Represents the natural vector posterior. */
    BatchOutput naturalVectorPosterior = null;
    private boolean activateOutput = false;

    /** Introduce Parameter Constraints**/
    Constraints constraints = new Constraints();

    /** Store weather parallel message passing will be employed or not.**/
    private boolean parallelMode = false;

    /**
     * Returns the window size.
     * @return the window size.
     */
    public int getWindowsSize() {
        return windowsSize;
    }

    /**
     * Sets a random restart for this SVB.
     * @param randomRestart {@code true} if a random restart is to be set, {@code false} otherwise.
     */
    public void setRandomRestart(boolean randomRestart) {
        this.randomRestart = randomRestart;
    }

    /**
     * Returns the plateu structure of this SVB.
     * @return a {@link PlateuStructure} object.
     */
    public PlateuStructure getPlateuStructure() {
        return plateuStructure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPlateuStructure(PlateuStructure plateuStructure) {
        this.plateuStructure = plateuStructure;
    }

    /**
     * Returns the seed.
     * @return the seed.
     */
    public int getSeed() {
        return seed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    }

    /**
     * Sets the transition method of this SVB.
     * @param transitionMethod a valid {@link TransitionMethod} object.
     */
    public void setTransitionMethod(TransitionMethod transitionMethod) {
        this.transitionMethod = transitionMethod;
    }

    /**
     * Returns the transition method of this SVB.
     * @param <E> the type of elements.
     * @return a valid {@link TransitionMethod} object.
     */
    public <E extends TransitionMethod> E getTransitionMethod() {
        return (E)this.transitionMethod;
    }

    /**
     * Returns the natural parameter priors.
     * @return a {@link CompoundVector} including the natural parameter priors.
     */
    public CompoundVector getNaturalParameterPrior(){
        return this.computeNaturalParameterVectorPrior();
        /*
        if (naturalVectorPrior==null){
            naturalVectorPrior = this.computeNaturalParameterVectorPrior();
        }
        return naturalVectorPrior;
        */
    }

    /**
     * Returns the natural parameter posterior.
     * @return a {@link BatchOutput} object including the natural parameter posterior.
     */
    protected BatchOutput getNaturalParameterPosterior() {
        if (this.naturalVectorPosterior==null){
            naturalVectorPosterior = new BatchOutput(this.computeNaturalParameterVectorPrior(), 0);
        }
        return naturalVectorPosterior;
    }

    /**
     * Computes the natural parameter priors.
     * @return a {@link CompoundVector} including the natural parameter priors.
     */
    protected CompoundVector computeNaturalParameterVectorPrior(){
        return this.getPlateuStructure().getPlateauNaturalParameterPrior();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {
        this.initLearning();
        if (!nonSequentialModel) {
            this.elbo = this.dataStream.streamOfBatches(this.windowsSize).mapToDouble(this::updateModel).sum();
        }else {
            this.elbo = this.dataStream.streamOfBatches(this.windowsSize).mapToDouble(this::updateModelParallel).sum();
       }
    }

    /**
     * Sets the model as a non sequential.
     * @param nonSequentialModel_ {@code true} if the model is to be set as a non sequential, {@code false} otherwise.
     */
    public void setNonSequentialModel(boolean nonSequentialModel_) {
        this.nonSequentialModel = nonSequentialModel_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutput(boolean activateOutput) {
        this.activateOutput=activateOutput;
        this.getPlateuStructure().getVMP().setOutput(activateOutput);
        this.getPlateuStructure().getVMP().setTestELBO(activateOutput);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double predictedLogLikelihood(DataOnMemory<DataInstance> batch) {
        this.getPlateuStructure().getNonReplictedNodes().forEach(node -> node.setActive(false));

        double elbo = 0;// this.getPlateuStructure().getNonReplictedNodes().mapToDouble(node -> this.getPlateuStructure().getVMP().computeELBO(node)).sum();

        elbo += this.updateModelOnBatchParallel(batch).getElbo();

        this.getPlateuStructure().getNonReplictedNodes().forEach(node -> node.setActive(true));

        return elbo;
    }

    /**
     * {@inheritDoc}
     */
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

        this.applyTransition();

        return elboBatch;
    }

    /**
     * Apply the transition method defined by the method setTransitionMethod.
     */
    public void applyTransition(){
        if (transitionMethod!=null)
            this.ef_extendedBN=this.transitionMethod.transitionModel(this.ef_extendedBN, this.plateuStructure);

    }

    /**
     * Updates the model sequentially using a given {@link DataOnMemory} object.
     * @param batch a {@link DataOnMemory} object.
     * @return a double value representing the log probability of an evidence.
     */
    private double updateModelSequential(DataOnMemory<DataInstance> batch) {
        nBatches++;
        //System.out.println("\n Batch:");
        this.plateuStructure.setEvidence(batch.getList());
        this.plateuStructure.runInference();
        nIterTotal+=this.plateuStructure.getVMP().getNumberOfIterations();

        this.updateNaturalParameterPrior(this.plateuStructure.getPlateauNaturalParameterPosterior());

        //this.plateuVMP.resetQs();
        return this.plateuStructure.getLogProbabilityOfEvidence();
    }

    /**
     * Updates the model on batch in parallel using a given {@link DataOnMemory} object.
     * @param batch a {@link DataOnMemory} object.
     * @return a {@link BatchOutput} object.
     */
    public BatchOutput updateModelOnBatchParallel(DataOnMemory<DataInstance> batch) {

        nBatches++;
        this.plateuStructure.setEvidence(batch.getList());
        this.plateuStructure.runInference();
        nIterTotal+=this.plateuStructure.getVMP().getNumberOfIterations();

        CompoundVector compoundVectorEnd = this.plateuStructure.getPlateauNaturalParameterPosterior();

        compoundVectorEnd.substract(this.getNaturalParameterPrior());

        return new BatchOutput(compoundVectorEnd, this.plateuStructure.getLogProbabilityOfEvidence());

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataPosterior> computePosterior(DataOnMemory<DataInstance> batch) {

        List<Variable> latentVariables = this.dag.getVariables().getListOfVariables().stream().filter(var -> var.getAttribute()!=null).collect(Collectors.toList());

        return this.computePosterior(batch, latentVariables);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataPosterior> computePosterior(DataOnMemory<DataInstance> batch, List<Variable> latentVariables) {
        Attribute seq_id = batch.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        this.plateuStructure.desactiveParametersNodes();

        this.plateuStructure.setEvidence(batch.getList());
        this.plateuStructure.runInference();

        this.plateuStructure.activeParametersNodes();

        List<DataPosterior> posteriors = new ArrayList<>();
        for (int i = 0; i < batch.getNumberOfDataInstances(); i++) {
            List<UnivariateDistribution> posteriorsQ = new ArrayList<>();
            for (Variable latentVariable : latentVariables) {
                posteriorsQ.add(plateuStructure.getEFVariablePosterior(latentVariable, i).deepCopy().toUnivariateDistribution());
            }
            posteriors.add(new DataPosterior((int) batch.getDataInstance(i).getValue(seq_id), posteriorsQ));
        }

        return posteriors;
    }


    public List<DataPosteriorAssignment> computePosteriorAssignment(DataOnMemory<DataInstance> batch, List<Variable> variables) {
        Attribute seq_id = batch.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        this.plateuStructure.desactiveParametersNodes();

        this.plateuStructure.setEvidence(batch.getList());
        this.plateuStructure.runInference();

        this.plateuStructure.activeParametersNodes();

        List<DataPosteriorAssignment> posteriors = new ArrayList<>();
        for (int i = 0; i < batch.getNumberOfDataInstances(); i++) {
            List<UnivariateDistribution> posteriorsQ = new ArrayList<>();
            Assignment assignment = new HashMapAssignment();
            for (Variable variable : variables) {
                EF_UnivariateDistribution dist = plateuStructure.getEFVariablePosterior(variable, i);
                if (dist!=null)
                    posteriorsQ.add(dist.deepCopy().toUnivariateDistribution());
                else
                    assignment.setValue(variable,batch.getDataInstance(i).getValue(variable));

            }
            DataPosterior dataPosterior = new DataPosterior((int) batch.getDataInstance(i).getValue(seq_id), posteriorsQ);
            posteriors.add(new DataPosteriorAssignment(dataPosterior,assignment));
        }

        return posteriors;
    }

    /**
     * Updates the model in parallel using a given {@link DataOnMemory} object.
     * @param batch a {@link DataOnMemory} object.
     * @return a double value.
     */
    private double updateModelParallel(DataOnMemory<DataInstance> batch) {

        nBatches++;
        this.plateuStructure.setEvidence(batch.getList());
        this.plateuStructure.runInference();
        nIterTotal+=this.plateuStructure.getVMP().getNumberOfIterations();

        CompoundVector compoundVectorEnd = this.plateuStructure.getPlateauNaturalParameterPosterior();

        compoundVectorEnd.substract(this.getNaturalParameterPrior());

        BatchOutput out = new BatchOutput(compoundVectorEnd, this.plateuStructure.getLogProbabilityOfEvidence());

        this.naturalVectorPosterior = BatchOutput.sumNonStateless(out, this.getNaturalParameterPosterior());

        return out.getElbo();
    }

    /**
     * Returns the number of batches.
     * @return the number of batches.
     */
    public int getNumberOfBatches() {
        return nBatches;
    }

    /**
     * Returns the average number of iterations.
     * @return the average number of iterations.
     */
    public double getAverageNumOfIterations(){
        return ((double)nIterTotal)/nBatches;
    }


    /**
     * Returns the associated DAG defining the PGM structure
     * @return A {@link DAG} object.
     */
    public DAG getDAG() {
        return dag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDAG(DAG dag) {
        this.dag = dag;
    }

    /**
     * Add a parameter constraint
     * @param constraint, a well defined object constraint.
     */
    public void addParameterConstraint(Constraint constraint){
        this.constraints.addConstraint(constraint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initLearning(){


        this.plateuStructure.initTransientDataStructure();

        this.getPlateuStructure().getVMP().setOutput(activateOutput);
        this.getPlateuStructure().getVMP().setTestELBO(activateOutput);
        this.getPlateuStructure().getVMP().setParallelMode(parallelMode);

        plateuStructure.setNRepetitions(windowsSize);

        this.nBatches = 0;
        this.nIterTotal = 0;
        this.plateuStructure.setSeed(seed);
        plateuStructure.setDAG(dag);
        plateuStructure.replicateModel();
        this.plateuStructure.resetQs();


        this.ef_extendedBN = this.plateuStructure.getEFLearningBN();


        if (transitionMethod!=null)
           this.ef_extendedBN = this.transitionMethod.initModel(this.ef_extendedBN, plateuStructure);

        this.constraints.setPlateuStructure(this.plateuStructure);
        this.constraints.buildConstrains();
    }

    @Override
    public void randomInitialize(){
        VMP original = this.getPlateuStructure().getVMP();

        VMPLocalUpdates vmp = new VMPLocalUpdates(this.getPlateuStructure());
        vmp.setMaxIter(this.getPlateuStructure().getVMP().getMaxIter());
        vmp.setThreshold(this.getPlateuStructure().getVMP().getThreshold());
        vmp.setTestELBO(this.getPlateuStructure().getVMP().isOutput());
        this.getPlateuStructure().setVmp(vmp);

        this.initLearning();

        vmp.init();

        CompoundVector posterior = this.getPlateuStructure().getPlateauNaturalParameterPosterior();

        this.getPlateuStructure().setVmp(original);

        this.initLearning();

        this.plateuStructure.updateNaturalParameterPosteriors(posterior);
    }

    public void randomInitialize(DataOnMemory<DataInstance> data){
        VMP original = this.getPlateuStructure().getVMP();

        VMPLocalUpdates vmp = new VMPLocalUpdates(this.getPlateuStructure());
        vmp.setMaxIter(this.getPlateuStructure().getVMP().getMaxIter());
        vmp.setThreshold(this.getPlateuStructure().getVMP().getThreshold());
        vmp.setTestELBO(this.getPlateuStructure().getVMP().isOutput());
        this.getPlateuStructure().setVmp(vmp);

        this.initLearning();

        this.getPlateuStructure().setEvidence(data.getList());

        vmp.init();

        CompoundVector posterior = this.getPlateuStructure().getPlateauNaturalParameterPosterior();

        this.getPlateuStructure().setVmp(original);

        this.initLearning();

        this.plateuStructure.updateNaturalParameterPosteriors(posterior);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataStream(DataStream<DataInstance> data) {
        this.dataStream=data;
    }

    /**
     * Updates the Natural Parameter Prior from a given parameter vector.
     * @param parameterVector a {@link CompoundVector} object.
     */
    public void updateNaturalParameterPrior(CompoundVector parameterVector){
        this.plateuStructure.updateNaturalParameterPrior(parameterVector);
        this.ef_extendedBN = this.plateuStructure.getEFLearningBN();
        this.naturalVectorPrior=this.computeNaturalParameterVectorPrior();
    }


    /**
     * Updas the parameters of the posteriors Qs distributions.
     * @param parameterVector object of the class CompoundVector
     */
    public void updateNaturalParameterPosteriors(CompoundVector parameterVector){
        this.plateuStructure.updateNaturalParameterPosteriors(parameterVector);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
//        if(!nonSequentialModel)
//            return new BayesianNetwork(this.dag, ef_extendedBN.toConditionalDistribution());
//        else{

            CompoundVector prior = this.plateuStructure.getPlateauNaturalParameterPrior();

            this.updateNaturalParameterPrior(this.plateuStructure.getPlateauNaturalParameterPosterior());

            BayesianNetwork learntBN =  new BayesianNetwork(this.dag, ef_extendedBN.toConditionalDistribution());

            this.updateNaturalParameterPrior(prior);

            return learntBN;
//        }
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
    public <E extends UnivariateDistribution> E getParameterPosterior(Variable parameter){
        return this.getPlateuStructure().getEFParameterPosterior(parameter).toUnivariateDistribution();
    }


    /**
     * Defines the batch Output.
     */
    public static class BatchOutput implements Serializable{

        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = 4107783324901370839L;

        CompoundVector vector;
        double elbo;

        public BatchOutput(CompoundVector vector_, double elbo_) {
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

        public static BatchOutput sumNonStateless(BatchOutput batchOutput1, BatchOutput batchOutput2){
            batchOutput2.getVector().sum(batchOutput1.getVector());
            batchOutput2.setElbo(batchOutput2.getElbo()+batchOutput1.getElbo());
            return batchOutput2;
        }

        public static BatchOutput sumStateless(BatchOutput batchOutput1, BatchOutput batchOutput2){
            BatchOutput sum = Serialization.deepCopy(batchOutput2);
            sum.getVector().sum(batchOutput1.getVector());
            sum.setElbo(batchOutput2.getElbo()+batchOutput1.getElbo());
            return sum;
        }
    }



}
