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

package eu.amidst.huginlink.learning;

import COM.hugin.HAPI.*;
import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.learning.parametric.ParallelMLMissingData;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.*;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.converters.BNConverterToHugin;
import eu.amidst.huginlink.inference.HuginInference;

import java.io.IOException;

/**
 * This class provides a link to the <a href="https://www.hugin.com">Hugin</a>'s functionality to learn in parallel a TAN model.
 * An important remark is that Hugin only allows to learn the TAN model for a data set completely loaded into RAM
 * memory. The case where our data set does not fit into memory, it solved in AMIDST in the following way. We learn
 * the structure using a smaller data set produced by <a href="https://en.wikipedia.org/wiki/Reservoir_sampling">Reservoir sampling</a>
 * and, then, we use AMIDST's {@link ParallelMaximumLikelihood} to learn the parameters of the TAN over the whole data set.
 *
 * <p> For further details about the implementation of the parallel TAN algorithm look at the following paper: </p>
 *
 * <i> Madsen, A.L. et al. A New Method for Vertical Parallelisation of TAN Learning Based on Balanced Incomplete
 * Block Designs. Probabilistic Graphical Models. Lecture Notes in Computer Science Volume 8754, 2014, pp 302-317. </i>
 */
public class ParallelTAN implements AmidstOptionsHandler {

    /** Represents the number of samples on memory. */
    private int numSamplesOnMemory;

    /** Represents the number of cores to be exploited during the parallel learning. */
    private int numCores;

    /** Represents the batch size. */
    private int batchSize;

    /** Represents the name of the variable acting as a root of the tree in the TAN model. */
    String nameRoot;

    /** Represents the name of the class variable in the TAN model */
    String nameTarget;

    /** Indicates if the learning is performed in parallel or not. */
    boolean parallelMode;

    /** Represents the learned BN**/
    BayesianNetwork learnedBN;

    /** Represents the inference engine for the predections*/
    HuginInference inference = null;

    /** Represents the class varible of the model*/
    private Variable targetVar;

    /**
     * Class constructor.
     */
    public ParallelTAN() {
    }

    /**
     * Sets the running mode, i.e., either parallel or sequential.
     * @param parallelMode true if the running mode is parallel, false otherwise.
     */
    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
        if (parallelMode) {
            //this.numCores = Runtime.getRuntime().availableProcessors();
        }
        else
            this.numCores=1;

    }

    /**
     * Returns the batch size to be used during the learning process.
     * @return the batch size.
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Sets the batch size.
     * @param batchSize the batch size.
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Returns the number of samples on memory.
     * @return the number of samples on memory.
     */
    public int getNumSamplesOnMemory() {
        return numSamplesOnMemory;
    }

    /**
     * Sets the number of samples on memory
     * @param numSamplesOnMemory_ the number of samples on memory
     */
    public void setNumSamplesOnMemory(int numSamplesOnMemory_) {
        this.numSamplesOnMemory = numSamplesOnMemory_;
    }

    /**
     * Returns the number of cores to be used during the learning process.
     * @return the number of cores.
     */
    public int getNumCores() {
        return numCores;
    }

    /**
     * Sets the number of cores to be used during the learning process.
     * @param numCores_ the number of cores.
     */
    public void setNumCores(int numCores_) {
        if(numCores_>Runtime.getRuntime().availableProcessors()) {
            this.numCores = Runtime.getRuntime().availableProcessors();
        }
        else {
            this.numCores = numCores_;
        }
    }

    /**
     * Sets the name of the variable acting as a root of the tree in the TAN model.
     * @param nameRoot the name of the root variable.
     */
    public void setNameRoot(String nameRoot) {
        this.nameRoot = nameRoot;
    }

    /**
     * Sets the name of the class variable in the TAN model.
     * @param nameTarget the name of the class variable.
     */
    public void setNameTarget(String nameTarget) {
        this.nameTarget = nameTarget;
    }

    /**
     * Gets the name of the class variable in the TAN model.
     */
    public String getNameTarget() {
        return this.nameTarget;
    }
    /**
     * Predicts the class membership probabilities for a given instance.
     * @param instance the data instance to be classified.
     * @return an array of doubles containing the estimated membership probabilities of the data instance for each class label.
     */
    public double[] predict(DataInstance instance) {
        if (learnedBN==null)
            throw new IllegalArgumentException("The model has not been learned");

        if (!Utils.isMissingValue(instance.getValue(targetVar)))
            System.out.println("Class Variable can not be set.");

        this.inference.setEvidence(instance);
        this.inference.runInference();
        Multinomial dist = this.inference.getPosterior(targetVar);

        return dist.getParameters();
    }

    /**
     * Learns a TAN structure from data using the Chow-Liu algorithm included in the Hugin API.
     * Parallel learning is performed only if the parallel mode was set to true.
     * @param dataStream a stream of data instances to be processed during the TAN structural learning.
     * @return a <code>DAG</code> structure in AMIDST format.
     * @throws ExceptionHugin
     */
    public DAG learnDAG(DataStream dataStream) throws ExceptionHugin {
        Variables modelHeader = new Variables(dataStream.getAttributes());
        this.targetVar = modelHeader.getVariableByName(this.nameTarget);
        DAG dag = new DAG(modelHeader);
        BayesianNetwork bn = new BayesianNetwork(dag);

        Domain huginNetwork = null;

        try {
            huginNetwork = BNConverterToHugin.convertToHugin(bn);

        } catch (ExceptionHugin exceptionHugin) {
            System.out.println("ParallelTan LearnDAG Error 1");
            exceptionHugin.printStackTrace();
            throw new IllegalStateException("Hugin Exception: " + exceptionHugin.getMessage());
        }
        DataOnMemory dataOnMemory = ReservoirSampling.samplingNumberOfSamples(this.numSamplesOnMemory, dataStream);

        // Set the number of cases
        int numCases = dataOnMemory.getNumberOfDataInstances();
        try {
            huginNetwork.setNumberOfCases(numCases);
            System.out.println("Learning DAG with " + this.numCores + " cores.");
            huginNetwork.setConcurrencyLevel(this.numCores);
        } catch (ExceptionHugin exceptionHugin) {
            System.out.println("ParallelTan LearnDAG Error 2");
            exceptionHugin.printStackTrace();
            throw new IllegalStateException("Hugin Exception: " + exceptionHugin.getMessage());
        }

        NodeList nodeList = huginNetwork.getNodes();

        // It is more efficient to loop the matrix of values in this way. 1st variables and 2nd cases
        for (int i = 0; i < nodeList.size(); i++) {
            Variable var = bn.getDAG().getVariables().getVariableById(i);
            Node n = nodeList.get(i);

            try {
                if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                    ((DiscreteChanceNode) n).getExperienceTable();
                    for (int j = 0; j < numCases; j++) {
                        double state = dataOnMemory.getDataInstance(j).getValue(var);
                        if (!Utils.isMissingValue(state)) {
                            ((DiscreteChanceNode) n).setCaseState(j, (int) state);
                        }
                    }
                } else {
                    ((ContinuousChanceNode) n).getExperienceTable();
                    for (int j = 0; j < numCases; j++) {
                        double value = dataOnMemory.getDataInstance(j).getValue(var);
                        if (!Utils.isMissingValue(value))
                            ((ContinuousChanceNode) n).setCaseValue(j, value);
                    }
                }
            }
            catch (ExceptionHugin exceptionHugin) {
                System.out.println("ParallelTan LearnDAG Error 3 with node " + Integer.toString(i));
                exceptionHugin.printStackTrace();
                throw new IllegalStateException("Hugin Exception: " + exceptionHugin.getMessage());
            }
//            if(i==0 || i==6) {
//                System.out.println("Node " + i);
//                System.out.println(Arrays.toString(n.getTable().getData()));
//
//                System.out.println(Arrays.toString (((DiscreteChanceNode) n).getExperienceTable().getData()));
//                System.out.println(((DiscreteChanceNode) n).getEnteredValue());
//                System.out.println(n.getChildren().stream().count());
//            }
        }

        //Structural learning
        DiscreteChanceNode root, target;
        try {
            root = (DiscreteChanceNode)huginNetwork.getNodeByName(nameRoot);
            target = (DiscreteChanceNode)huginNetwork.getNodeByName(nameTarget);

        } catch (ExceptionHugin exceptionHugin) {
            System.out.println("ParallelTan LearnDAG Error 4");
            exceptionHugin.printStackTrace();
            throw new IllegalStateException("Hugin Exception: " + exceptionHugin.getMessage());
        }


        try {
            //root.getHome().getNodes().stream().forEach(node -> System.out.println(node.getName())
            huginNetwork.learnChowLiuTree(root, target);
        } catch (ExceptionHugin exceptionHugin) {
            System.out.println("ParallelTan LearnDAG Error 5");
            exceptionHugin.printStackTrace();
            throw new IllegalStateException("Hugin Exception: " + exceptionHugin.getMessage());
        }


        DAG dagLearned;
        try {
            dagLearned = (BNConverterToAMIDST.convertToAmidst(huginNetwork)).getDAG();
            dagLearned.getVariables().setAttributes(dataStream.getAttributes());
        } catch (ExceptionHugin exceptionHugin) {
            System.out.println("ParallelTan LearnDAG Error 6");
            exceptionHugin.printStackTrace();
            throw new IllegalStateException("Hugin Exception: " + exceptionHugin.getMessage());
        }

        return dagLearned;

    }

    /**
     * Learns the parameters of a TAN structure using the {@link eu.amidst.core.learning.parametric.ParallelMaximumLikelihood}.
     * @param dataStream a stream of data instances for learning the parameters.
     * @return a <code>BayesianNetwork</code> object in ADMIST format.
     */
    public BayesianNetwork learn(DataStream<DataInstance> dataStream) {
        try {
            ParallelMLMissingData parameterLearningAlgorithm = new ParallelMLMissingData();
            parameterLearningAlgorithm.setLaplace(true);
            parameterLearningAlgorithm.setParallelMode(this.parallelMode);
            parameterLearningAlgorithm.setDAG(this.learnDAG(dataStream));
            parameterLearningAlgorithm.setDataStream(dataStream);
            parameterLearningAlgorithm.initLearning();
            parameterLearningAlgorithm.runLearning();
            learnedBN = parameterLearningAlgorithm.getLearntBayesianNetwork();

            this.inference = new HuginInference();
            this.inference.setModel(this.learnedBN);
            return this.learnedBN;
        }catch (ExceptionHugin ex){
            throw new IllegalStateException("Hugin Exception: " + ex.getMessage());
        }
    }

    /**
     * Learns the parameters of a TAN structure using the {@link eu.amidst.core.learning.parametric.ParallelMaximumLikelihood}.
     * @param dataStream a stream of data instances for learning the parameters.
     * @param batchSize the size of the batch for the parallel ML algorithm.
     * @return a <code>BayesianNetwork</code> object in ADMIST format.
     * @throws ExceptionHugin
     */
    public BayesianNetwork learn(DataStream<DataInstance> dataStream, int batchSize) throws ExceptionHugin {
        ParallelMLMissingData parameterLearningAlgorithm = new ParallelMLMissingData();
        parameterLearningAlgorithm.setWindowsSize(batchSize);
        parameterLearningAlgorithm.setParallelMode(this.parallelMode);
        parameterLearningAlgorithm.setDAG(this.learnDAG(dataStream));
        parameterLearningAlgorithm.setDataStream(dataStream);
        parameterLearningAlgorithm.initLearning();
        parameterLearningAlgorithm.runLearning();
        learnedBN = parameterLearningAlgorithm.getLearntBayesianNetwork();

        this.inference = new HuginInference();
        this.inference.setModel(this.learnedBN);
        return this.learnedBN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String listOptions(){

        return  this.classNameID() +",\\"+
                "-numSamplesOnMemory, 1000, Number of samples on memory\\" +
                "-numCores,"+Runtime.getRuntime().availableProcessors()+", Number of cores\\" +
                "-windowsSize, 1000, Batch size\\"+
                "-nameRoot, root, Name of root variable.\\" +
                "-nameTarget, target, Name of target variable\\" +
                "-parallelMode, true, Run in parallel\\";
    }

    /**
     * {@inheritDoc}
     */
    public void loadOptions(){
        this.setNumSamplesOnMemory(this.getIntOption("-numSamplesOnMemory"));
        this.setNumCores(this.getIntOption("-numCores"));
        this.setBatchSize(this.getIntOption("-windowsSize"));
        this.setNameRoot(this.getOption("-nameRoot"));
        this.setNameTarget(this.getOption("-nameTarget"));
        this.setParallelMode(this.getBooleanOption("-parallelMode"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String listOptionsRecursively() {
        return this.listOptions()
                + "\n" + BayesianNetwork.listOptionsRecursively()
                + "\n" + AmidstOptionsHandler.listOptionsRecursively(BayesianNetworkSampler.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String classNameID() {
        return "ParallelTAN";
    }


    public static void main(String[] args) throws IOException {


        OptionParser.setArgsOptions(ParallelTAN.class, args);

        BayesianNetworkGenerator.loadOptions();

        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(2000, 10);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        int sampleSize = 5000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.loadOptions();

        DataStream<DataInstance> data =  sampler.sampleToDataStream(sampleSize);

        for (int i = 1; i <= 4; i++) {
            int samplesOnMemory = 1000;
            int numCores = i;
            System.out.println("Learning TAN: " + samplesOnMemory + " samples on memory, " + numCores + "core/s ...");
            ParallelTAN tan = new ParallelTAN();
            tan.setOptions(args);
            //tan.loadOptionsFromFile("configurationFiles/conf.txt");
            tan.setNumCores(numCores);
            tan.setNumSamplesOnMemory(samplesOnMemory);
            tan.setNameRoot(bn.getVariables().getListOfVariables().get(0).getName());
            tan.setNameTarget(bn.getVariables().getListOfVariables().get(1).getName());
            Stopwatch watch = Stopwatch.createStarted();
            BayesianNetwork model = tan.learn(data);
            System.out.println(watch.stop());
        }
    }
}



