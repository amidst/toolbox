package eu.amidst.core.huginlink;

import COM.hugin.HAPI.*;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnMemory;
import eu.amidst.core.database.DataOnStream;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.ReservoirSampling;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;


/**
 * Created by afa on 9/12/14.
 */
public class ParallelTAN {

     public static BayesianNetwork learn(DataOnStream dataOnStream, String nameRoot, String nameTarget) throws ExceptionHugin {

        StaticVariables modelHeader = new StaticVariables(dataOnStream.getAttributes());
        DAG dag = new DAG(modelHeader);
        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);
        Domain huginNetwork = ConverterToHugin.convertToHugin(bn);

        DataOnMemory dataOnMemory = ReservoirSampling.samplingNumberOfSamples(1000,dataOnStream);

        // Set the number of cases
        int numCases = dataOnMemory.getNumberOfDataInstances();
        huginNetwork.setNumberOfCases(numCases);

        // Set the number of cores
        int cores = Runtime.getRuntime().availableProcessors();
        huginNetwork.setConcurrencyLevel(cores);

        NodeList nodeList = huginNetwork.getNodes();


        // It is more efficient to loop the matrix of values in this way. 1st variables and 2nd cases
        for (int i = 0;i<nodeList.size();i++) {
            Node n = nodeList.get(i);
            if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                for (int j=0;j<numCases;j++){
                    Variable var =  bn.getDAG().getStaticVariables().getVariableById(i);
                    DataInstance dataInstance = dataOnMemory.getDataInstance(j);
                    int state = (int)dataInstance.getValue(var);
                    ((DiscreteNode)n).setCaseState(j, state);
                }
            } else {
                for (int j=0;j<numCases;j++){
                    double value = dataOnMemory.getDataInstance(j).getValue(bn.getDAG().getStaticVariables().getVariableById(i));
                    ((ContinuousChanceNode)n).setCaseValue(j, (long) value);
                }
            }
        }

        Node root = huginNetwork.getNodeByName(nameRoot);
        Node target = huginNetwork.getNodeByName(nameTarget);

        huginNetwork.learnChowLiuTree(root,target);

        huginNetwork.setMaxNumberOfEMIterations(1000);
        huginNetwork.learnTables();

        huginNetwork.saveAsNet(new String("parallelTAN.net"));

        return(ConverterToAMIDST.convertToAmidst(huginNetwork));
    }

    public static void main(String[] args) throws ExceptionHugin {

        WekaDataFileReader fileReader = new WekaDataFileReader(new String("datasets/syntheticData.arff"));
        StaticDataOnDiskFromFile data = new StaticDataOnDiskFromFile(fileReader);

        ParallelTAN.learn(data, "A", "B");
    }
}




