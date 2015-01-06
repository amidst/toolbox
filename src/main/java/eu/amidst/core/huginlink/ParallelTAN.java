package eu.amidst.core.huginlink;


import COM.hugin.HAPI.*;
import com.google.common.base.Stopwatch;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnMemory;
import eu.amidst.core.database.DataOnStream;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkWriter;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.ReservoirSampling;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;


/**
 * Created by afa on 9/12/14.
 */
public class ParallelTAN {

    private int numSamplesOnMemory;
    private int numCores;

    public ParallelTAN () {
        this.numSamplesOnMemory = 10000;
        this.numCores = 1; // Or Runtime.getRuntime().availableProcessors();
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


    public BayesianNetwork learn(DataOnStream dataOnStream, String nameRoot, String nameTarget) throws ExceptionHugin {

        StaticVariables modelHeader = new StaticVariables(dataOnStream.getAttributes());
        DAG dag = new DAG(modelHeader);
        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);
        Domain huginNetwork = ConverterToHugin.convertToHugin(bn);

        DataOnMemory dataOnMemory = ReservoirSampling.samplingNumberOfSamples(this.numSamplesOnMemory,dataOnStream);




        // Set the number of cases
        int numCases = dataOnMemory.getNumberOfDataInstances();
        huginNetwork.setNumberOfCases(numCases);

        huginNetwork.setConcurrencyLevel(this.numCores);

        NodeList nodeList = huginNetwork.getNodes();

        // It is more efficient to loop the matrix of values in this way. 1st variables and 2nd cases
        for (int i = 0;i<nodeList.size();i++) {
            Variable var =  bn.getDAG().getStaticVariables().getVariableById(i);
            Node n = nodeList.get(i);
            if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                ((DiscreteChanceNode)n).getExperienceTable();
                for (int j=0;j<numCases;j++){
                    int state = (int)dataOnMemory.getDataInstance(j).getValue(var);
                    ((DiscreteChanceNode)n).setCaseState(j, state);
                }
            } else {
                ((ContinuousChanceNode)n).getExperienceTable();
                for (int j=0;j<numCases;j++){
                    double value = dataOnMemory.getDataInstance(j).getValue(var);
                    ((ContinuousChanceNode)n).setCaseValue(j, (long) value);
                }
            }
        }

        Stopwatch watch = Stopwatch.createStarted();
        //Structural learning
        Node root = huginNetwork.getNodeByName(nameRoot);
        Node target = huginNetwork.getNodeByName(nameTarget);

        huginNetwork.learnChowLiuTree(root, target);

        //Parametric learning
        huginNetwork.compile();
        huginNetwork.learnTables();
        huginNetwork.uncompile();

        System.out.println("Only TAN : "+watch.stop());

        return (ConverterToAMIDST.convertToAmidst(huginNetwork));
    }





}




