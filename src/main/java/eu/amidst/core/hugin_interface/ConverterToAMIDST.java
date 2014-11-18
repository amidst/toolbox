package eu.amidst.core.hugin_interface;

import COM.hugin.HAPI.*;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.header.StateSpaceType;
import eu.amidst.core.header.StaticModelHeader;
import eu.amidst.core.header.VariableBuilder;
import eu.amidst.core.modelstructure.BayesianNetwork;
import eu.amidst.core.modelstructure.DAG;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 14/11/14.
 */
public class ConverterToAMIDST {

    private BayesianNetwork amidstNetwork;

    public ConverterToAMIDST (){

    }

    public void setNodes(NodeList huginNodes){

        Attribute att = new Attribute(0,"A","A",StateSpaceType.MULTINOMIAL,2);
        VariableBuilder builder = new VariableBuilder(att);

        List<Attribute> atts = new ArrayList<Attribute>();

        int numNodes = huginNodes.size();

        for(int i=0;i<numNodes;i++){
            Node n = (Node)huginNodes.get(i);

            try {
                if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                    int numStates = (int)((DiscreteChanceNode)n).getNumberOfStates();
                    atts.add(new Attribute(i, n.getName(), " ", StateSpaceType.MULTINOMIAL, numStates));
                }
                else if (n.getKind().compareTo(NetworkModel.H_KIND_CONTINUOUS) == 0) {
                    atts.add(new Attribute(i, n.getName(), n.getName(), StateSpaceType.REAL, 0));
                }
            }
            catch (ExceptionHugin e) {
                System.out.println("Exception caught: " + e.getMessage());
            }
        }

        StaticModelHeader modelHeader = new StaticModelHeader(new Attributes(atts));
        DAG dag = new DAG(modelHeader);
        BayesianNetwork.newBayesianNetwork(dag);

    }





    public static void main(String args[]) {




    }

}
