package eu.amidst.huginlink.learning;


import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import COM.hugin.HAPI.*;
import com.google.common.base.Stopwatch;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataOnMemory;
import eu.amidst.core.database.StaticDataInstance;
import eu.amidst.core.learning.LearningEngineForBN;
import eu.amidst.core.learning.MaximumLikelihoodForBN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.*;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.converters.BNConverterToHugin;

import java.io.IOException;


/**
 * Created by afa on 9/12/14.
 */
public class ParallelTAN implements AmidstOptionsHandler {


    private int numSamplesOnMemory;
    private int numCores = Runtime.getRuntime().availableProcessors();
    private int batchSize;
    String nameRoot;
    String nameTarget;
    boolean parallelMode;




    public ParallelTAN() {
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
        if (parallelMode)
            this.numCores = Runtime.getRuntime().availableProcessors();
        else
            this.numCores=1;

    }

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

    public int getNumCores() {
        return numCores;
    }

    public void setNumCores(int numCores_) {
        this.numCores = numCores_;
    }

    public void setNameRoot(String nameRoot) {
        this.nameRoot = nameRoot;
    }

    public void setNameTarget(String nameTarget) {
        this.nameTarget = nameTarget;
    }

    public DAG learnDAG(DataBase dataBase) throws ExceptionHugin {
        StaticVariables modelHeader = new StaticVariables(dataBase.getAttributes());
        DAG dag = new DAG(modelHeader);
        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);


        Domain huginNetwork = null;


        try {
            huginNetwork = BNConverterToHugin.convertToHugin(bn);

            DataOnMemory dataOnMemory = ReservoirSampling.samplingNumberOfSamples(this.numSamplesOnMemory, dataBase);

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

            //Parametric learning
            //huginNetwork.compile();
            //huginNetwork.learnTables();
            //huginNetwork.uncompile();


            return (BNConverterToAMIDST.convertToAmidst(huginNetwork)).getDAG();
        } catch (ExceptionHugin exceptionHugin) {
            throw new IllegalStateException("Hugin Exception: " + exceptionHugin.getMessage());
        }
    }


    public BayesianNetwork learnBN(DataBase<StaticDataInstance> dataBase) throws ExceptionHugin {

        //TODO uncomment this and solve the problem
        //LearningEngine.setStaticStructuralLearningAlgorithm(this::learnDAG);
        MaximumLikelihoodForBN.setBatchSize(this.batchSize);
        MaximumLikelihoodForBN.setParallelMode(this.parallelMode);
        LearningEngineForBN.setStaticParameterLearningAlgorithm(MaximumLikelihoodForBN::learnParametersStaticModel);

        return LearningEngineForBN.learnStaticModel(dataBase);
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
        return "eu.amidst.huginlink.learning.ParallelTAN";
    }



    public static void main(String[] args) throws ExceptionHugin, IOException {


        OptionParser.setArgsOptions(ParallelTAN.class,args);

        BayesianNetworkGenerator.loadOptions();

        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(2000);
        BayesianNetworkGenerator.setNumberOfStates(10);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        int sampleSize = 5000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.loadOptions();

        sampler.setParallelMode(true);
        DataBase<StaticDataInstance> data =  sampler.sampleToDataBase(sampleSize);

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



