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


        //ParseListener parseListener = new DefaultClassParseListener();
        //Domain huginNetwork = new Domain(new String("networks/parallelTAN.net"), parseListener);




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
            Variable var =  bn.getDAG().getStaticVariables().getVariableById(i);
            Node n = (Node)nodeList.get(i);
            if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                for (int j=0;j<numCases;j++){
                    int state = (int)dataOnMemory.getDataInstance(j).getValue(var);
                    ((DiscreteChanceNode)n).setCaseState(j, state);
                }
            } else {
                for (int j=0;j<numCases;j++){
                    double value = dataOnMemory.getDataInstance(j).getValue(var);
                    ((ContinuousChanceNode)n).setCaseValue(j, (long) value);
                }
            }
        }
        System.out.println("Number of cases:" + huginNetwork.getNumberOfCases());

        Node root = huginNetwork.getNodeByName(nameRoot);
        Node target = huginNetwork.getNodeByName(nameTarget);
        huginNetwork.setSignificanceLevel(0.05);
        huginNetwork.learnChowLiuTree(root, target);



        huginNetwork = new Domain(new String("networks/parallelTAN.net"),new DefaultClassParseListener());




     //   ((DataSet)huginNetwork.getUserData()).saveAsCSV("datasets/dataFromHugin.csv",new char[]);
        huginNetwork.compile();
//        huginNetwork.setMaxNumberOfEMIterations(1000);
        huginNetwork.learnTables();


        huginNetwork.uncompile();


        huginNetwork.saveAsNet(new String("networks/parallelTAN.net"));



        return (ConverterToAMIDST.convertToAmidst(huginNetwork));
    }


    public static void main(String[] args) throws ExceptionHugin {

        WekaDataFileReader fileReader = new WekaDataFileReader(new String("datasets/syntheticDataCat.arff"));
        StaticDataOnDiskFromFile data = new StaticDataOnDiskFromFile(fileReader);

        ParallelTAN.learn(data, "A", "B");

    }
}




