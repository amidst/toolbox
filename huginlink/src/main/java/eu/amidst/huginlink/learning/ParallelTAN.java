package eu.amidst.huginlink.learning;

import COM.hugin.HAPI.*;
import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.learning.parametric.LearningEngine;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.*;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.converters.BNConverterToHugin;

import java.io.IOException;


/**
 * This class implements the parallel structural learning of a TAN model using the Hugin API. Parameter learning is
 * performed using the AMIDST implementation through <code>MaximumLikelihoodForBN</code> class.
 *
 * @author Ana M. Martínez
 * @version 1.0
 * @since 9/12/15
 */
public class ParallelTAN implements AmidstOptionsHandler {

    /**
     *
     */
    private int numSamplesOnMemory;

    /**
     * Number of cores to be exploited during the parallel learning
     */
    private int numCores;

    /**
     *
     */
    private int batchSize;

    /**
     * Name of the variable acting as a root of the tree in the TAN model.
     */
    String nameRoot;

    /**
     * Name of the class variable in the TAN model
     */
    String nameTarget;
    /**
     * Indicates if the learning is executed in parallel exploiting the cores.
     */
    boolean parallelMode;

    /**
     * Class constructor.
     */
    public ParallelTAN() {
    }

    /**
     * Sets the execution mode: parallel or sequential.
     *
     * @param parallelMode a boolean indicating if the execution mode in parallel.
     */
    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
        if (parallelMode)
            this.numCores = Runtime.getRuntime().availableProcessors();
        else
            this.numCores=1;

    }

    /**
     * Gets the batch size to be used during learning.
     * @return
     */
    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    public int getNumSamplesOnMemory() {
        return numSamplesOnMemory;
    }

    public void setNumSamplesOnMemory(int numSamplesOnMemory_) {
        this.numSamplesOnMemory = numSamplesOnMemory_;
    }

    /**
     * Gets the number of cores used during learning.
     * @return the number of cores.
     */
    public int getNumCores() {
        return numCores;
    }

    /**
     * Sets the number of cores to be used during learning.
     * @param numCores_
     */
    public void setNumCores(int numCores_) {
        this.numCores = numCores_;
    }

    /**
     * Sets the name of the variable acting as a root of the tree in the TAN model
     * @param nameRoot the name of the variable
     */
    public void setNameRoot(String nameRoot) {
        this.nameRoot = nameRoot;
    }

    /**
     * Sets the name of the class variable in the TAN model.
     * @param nameTarget the name of the variable
     */
    public void setNameTarget(String nameTarget) {
        this.nameTarget = nameTarget;
    }

    /**
     * Learns a TAN structure from data using the Chow-Liu algorithm included in the Hugin API. Parallel learning is
     * used only if the parallel mode is set to true.
     * @param dataStream a stream of data instances to be processed during learning
     * @return a <code>DAG</code> structure in AMIDST format.
     * @throws ExceptionHugin
     */
    public DAG learnDAG(DataStream dataStream) throws ExceptionHugin {
        Variables modelHeader = new Variables(dataStream.getAttributes());
        DAG dag = new DAG(modelHeader);
        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

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
                Variable var = bn.getDAG().getStaticVariables().getVariableById(i);
                Node n = nodeList.get(i);
                if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                    ((DiscreteChanceNode) n).getExperienceTable();
                    for (int j = 0; j < numCases; j++) {
                        int state = (int) dataOnMemory.getDataInstance(j).getValue(var);
                        ((DiscreteChanceNode) n).setCaseState(j, state);
                    }
                } else {
                    ((ContinuousChanceNode) n).getExperienceTable();
                    for (int j = 0; j < numCases; j++) {
                        double value = dataOnMemory.getDataInstance(j).getValue(var);
                        ((ContinuousChanceNode) n).setCaseValue(j, (long) value);
                    }
                }
            }

            //Structural learning
            Node root = huginNetwork.getNodeByName(nameRoot);
            Node target = huginNetwork.getNodeByName(nameTarget);

            Stopwatch watch = Stopwatch.createStarted();
            huginNetwork.learnChowLiuTree(root, target);
            System.out.println("Structural Learning in Hugin: " + watch.stop());

            return (BNConverterToAMIDST.convertToAmidst(huginNetwork)).getDAG();
        } catch (ExceptionHugin exceptionHugin) {
            throw new IllegalStateException("Hugin Exception: " + exceptionHugin.getMessage());
        }
    }

    /**
     * Learns the parameters of a TAN structure by maximum likelihood using the AMIDST implementation.
     * @param dataStream a stream of data instances for learning the parameters.
     * @return a <code>BayesianNetwork</code> object in ADMIST format.
     * @throws ExceptionHugin
     */
    public BayesianNetwork learnBN(DataStream<DataInstance> dataStream) throws ExceptionHugin {

        LearningEngine.setParallelMode(this.parallelMode);
        LearningEngine.setParameterLearningAlgorithm(new ParallelMaximumLikelihood());

        return LearningEngine.learnParameters(this.learnDAG(dataStream), dataStream);
    }

    @Override
    public String listOptions(){

        return  this.classNameID() +",\\"+
                "-numSamplesOnMemory, 1000, Number of samples on memory\\" +
                "-numCores,"+Runtime.getRuntime().availableProcessors()+", Number of cores\\" +
                "-batchSize, 1000, Batch size\\"+
                "-nameRoot, root, Name of root variable.\\" +
                "-nameTarget, target, Name of target variable\\" +
                "-parallelMode, true, Run in parallel\\";
    }

    public void loadOptions(){
        this.setNumSamplesOnMemory(this.getIntOption("-numSamplesOnMemory"));
        this.setNumCores(this.getIntOption("-numCores"));
        this.setBatchSize(this.getIntOption("-batchSize"));
        this.setNameRoot(this.getOption("-nameRoot"));
        this.setNameTarget(this.getOption("-nameTarget"));
        this.setParallelMode(this.getBooleanOption("-parallelMode"));
    }

    @Override
    public String listOptionsRecursively() {
        return this.listOptions()
                + "\n" + BayesianNetwork.listOptionsRecursively()
                + "\n" + AmidstOptionsHandler.listOptionsRecursively(BayesianNetworkSampler.class);
    }

    @Override
    public String classNameID() {
        return "ParallelTAN";
    }



    public static void main(String[] args) throws ExceptionHugin, IOException {


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
            tan.setNameRoot(bn.getStaticVariables().getListOfVariables().get(0).getName());
            tan.setNameTarget(bn.getStaticVariables().getListOfVariables().get(1).getName());
            Stopwatch watch = Stopwatch.createStarted();
            BayesianNetwork model = tan.learnBN(data);
            System.out.println(watch.stop());
        }
    }
}



