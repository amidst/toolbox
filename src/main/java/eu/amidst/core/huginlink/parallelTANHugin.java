package eu.amidst.core.huginlink;


import COM.hugin.HAPI.*;
import eu.amidst.core.database.DataOnMemory;
import eu.amidst.core.database.filereaders.DataFileReader;
import eu.amidst.core.database.filereaders.StaticDataOnMemoryFromFile;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;


/**
 * Created by afa on 9/12/14.
 */
public class parallelTANHugin {

    BayesianNetwork amidstTAN;

    public parallelTANHugin(DataFileReader fileReader) throws ExceptionHugin {


        StaticVariables modelHeader = new StaticVariables(fileReader.getAttributes());

        DAG dag = new DAG(modelHeader);
        StaticVariables variables = dag.getStaticVariables();

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        ConverterToHugin converterToHugin = new ConverterToHugin(bn);

        converterToHugin.convertToHuginBN();

        Domain huginNetwork = converterToHugin.getHuginNetwork();

        StaticDataOnMemoryFromFile data = new StaticDataOnMemoryFromFile(fileReader);

        // Set the number of cores
        int numCases = data.getNumberOfDataInstances();
        huginNetwork.setNumberOfCases(numCases);

        // Set the number of cores
        int cores = Runtime.getRuntime().availableProcessors();
        huginNetwork.setConcurrencyLevel(cores);


        NodeList nodeList = huginNetwork.getNodes();


        // It is more efficient to loop the matrix of values in this way. 1st variables and 2nd cases
        for (int i = 0;i<nodeList.size();i++) {
            Node n = (Node) nodeList.get(i);
            if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                for (int j=0;j<numCases;j++){
                    int state = (int)data.getDataInstance(j).getValue(bn.getDAG().getStaticVariables().getVariableById(i));
                    ((DiscreteNode)n).setCaseState(j, state);
                }
            } else {
                for (int j=0;j<numCases;j++){
                    double value = data.getDataInstance(j).getValue(bn.getDAG().getStaticVariables().getVariableById(i));
                    ((ContinuousChanceNode)n).setCaseValue(j, (long) value);
                }
            }
        }

        Node target = (Node)nodeList.get(0);
        Node root = (Node)nodeList.get(1);


        //huginBN.setSignificanceLevel(0.05);

        //  huginNetwork.learnChowLiuTree(root, target);

        //huginBN.setMaxNumberOfEMIterations(1000);
        //  huginNetwork.learnTables();

        huginNetwork.saveAsNet(new String("tan.net"));

        ConverterToAMIDST converterToAMIDST = new ConverterToAMIDST(huginNetwork);
        converterToAMIDST.convertToAmidstBN();
        this.amidstTAN = converterToAMIDST.getAmidstNetwork();
    }


    public static void main(String[] args) throws ExceptionHugin {

        WekaDataFileReader fileReader = new WekaDataFileReader(new String("datasets/syntheticData.arff"));
        parallelTANHugin tan = new parallelTANHugin(fileReader);

    }
}




