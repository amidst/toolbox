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
 * This class provides a link to the <a href="https://www.hugin.com">Hugin</a>'s functionality to learn in parallel
 * the structure of a Bayesian network model from data using the PC algorithm.
 *
 * An important remark is that Hugin only allows to apply the PC algorithm over a data set completely loaded into RAM
 * memory. The case where our data set does not fit into memory, it solved in AMIDST in the following way. We learn
 * the structure using a smaller data set produced by <a href="https://en.wikipedia.org/wiki/Reservoir_sampling">Reservoir sampling</a>
 * and, then, we use AMIDST's {@link ParallelMaximumLikelihood} to learn the parameters of the BN model over the
 * whole data set.
 *
 * <p> For further details about the implementation of the parallel PC algorithm look at the following paper: </p>
 *
 * <i> Madsen, A. L., Jensen, F., Salmerón, A., Langseth, H., Nielsen, T. D. (2015). Parallelization of the PC
 * Algorithm (2015). The XVI Conference of the Spanish Association for Artificial Intelligence (CAEPIA'15), pages 14-24 </i>
 */
public class ParallelPC implements AmidstOptionsHandler {

    /** Represents the number of samples on memory. */
    private int numSamplesOnMemory;

    /** Represents the number of cores to be exploited during the parallel learning. */
    private int numCores;

    /** Represents the batch size. */
    private int batchSize;

    /** Indicates if the learning is performed in parallel or not. */
    boolean parallelMode;

    /** Represents the learned BN**/
    BayesianNetwork learnedBN;

    /** Represents the inference engine for the predections*/
    HuginInference inference = null;

   /**
     * Class constructor.
     */
    public ParallelPC() {
    }

    /**
     * Sets the running mode, i.e., either parallel or sequential.
     * @param parallelMode true if the running mode is parallel, false otherwise.
     */
    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
        if (parallelMode)
            this.numCores = Runtime.getRuntime().availableProcessors();
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
        this.numCores = numCores_;
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
        DAG dag = new DAG(modelHeader);
        BayesianNetwork bn = new BayesianNetwork(dag);

        Domain huginNetwork = null;

        try {
            huginNetwork = BNConverterToHugin.convertToHugin(bn);

            DataOnMemory dataOnMemory = ReservoirSampling.samplingNumberOfSamples(this.numSamplesOnMemory, dataStream);

            // Set the number of cases
            int numCases = dataOnMemory.getNumberOfDataInstances();
            huginNetwork.setNumberOfCases(numCases);
            huginNetwork.setConcurrencyLevel(this.numCores);
            NodeList nodeList = huginNetwork.getNodes();

            // It is more efficient to loop the matrix of values in this way. 1st variables and 2nd cases
            for (int i = 0; i < nodeList.size(); i++) {
                Variable var = bn.getDAG().getVariables().getVariableById(i);
                Node n = nodeList.get(i);
                if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                    ((DiscreteChanceNode) n).getExperienceTable();
                    for (int j = 0; j < numCases; j++) {
                        double state = dataOnMemory.getDataInstance(j).getValue(var);
                        if (!Utils.isMissingValue(state))
                            ((DiscreteChanceNode) n).setCaseState(j, (int)state);
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

            //Structural learning
            Stopwatch watch = Stopwatch.createStarted();
            huginNetwork.learnStructureNPC();
            System.out.println("Structural Learning in Hugin: " + watch.stop());

            DAG dagLearned = (BNConverterToAMIDST.convertToAmidst(huginNetwork)).getDAG();
            dagLearned.getVariables().setAttributes(dataStream.getAttributes());
            return dagLearned;
        } catch (ExceptionHugin exceptionHugin) {
            throw new IllegalStateException("Hugin Exception: " + exceptionHugin.getMessage());
        }
    }

    /**
     * Learns the parameters of a TAN structure using the {@link ParallelMaximumLikelihood}.
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
     * Learns the parameters of a TAN structure using the {@link ParallelMaximumLikelihood}.
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
                "-parallelMode, true, Run in parallel\\";
    }

    /**
     * {@inheritDoc}
     */
    public void loadOptions(){
        this.setNumSamplesOnMemory(this.getIntOption("-numSamplesOnMemory"));
        this.setNumCores(this.getIntOption("-numCores"));
        this.setBatchSize(this.getIntOption("-windowsSize"));
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


        OptionParser.setArgsOptions(ParallelPC.class, args);

        BayesianNetworkGenerator.loadOptions();

        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(100, 10);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        int sampleSize = 5000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.loadOptions();

        DataStream<DataInstance> data =  sampler.sampleToDataStream(sampleSize);

        for (int i = 1; i <= 4; i++) {
            int samplesOnMemory = 1000;
            int numCores = i;
            System.out.println("Learning PC: " + samplesOnMemory + " samples on memory, " + numCores + "core/s ...");
            ParallelPC parallelPC = new ParallelPC();
            parallelPC.setOptions(args);
            //tan.loadOptionsFromFile("configurationFiles/conf.txt");
            parallelPC.setNumCores(numCores);
            parallelPC.setNumSamplesOnMemory(samplesOnMemory);
            Stopwatch watch = Stopwatch.createStarted();
            BayesianNetwork model = parallelPC.learn(data);
            System.out.println(watch.stop());
        }
    }
}



