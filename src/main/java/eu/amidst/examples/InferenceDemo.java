package eu.amidst.examples;


import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.huginlink.*;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * Created by afa on 13/1/15.
 */
public class InferenceDemo {

    public static void printCPTs (Domain domainObject) throws ExceptionHugin {

        System.out.println("-------------------------------");
        System.out.println("CONDITIONAL PROBABILITY TABLES:");
        System.out.println("-------------------------------");

        domainObject.getNodes().stream().forEach((node) -> {
            try {
                System.out.println(node.getName() + ": " + Arrays.toString(node.getTable().getData()));
            } catch (ExceptionHugin exceptionHugin) {
                exceptionHugin.printStackTrace();
            }
        });
     }

    public static void printBeliefs (Domain domainObject) throws ExceptionHugin {

        System.out.println("--------");
        System.out.println("BELIEFS:");
        System.out.println("--------");

        domainObject.getNodes().stream().forEach((node) -> {
            try {
                System.out.print("\n" + node.getName()+ ": ");
                int numStates = (int)((LabelledDCNode)node).getNumberOfStates();
                for (int j=0;j<numStates;j++){
                    System.out.print(((LabelledDCNode) node).getBelief(j) + " ");
                }

            } catch (ExceptionHugin exceptionHugin) {
                exceptionHugin.printStackTrace();
            }
        });


//        NodeList nodes = domainObject.getNodes();
//        int numNodes = nodes.size();
//        System.out.println("--------");
//        System.out.println("BELIEFS:");
//        System.out.println("--------");
//
//
//
//        for(int i=0;i<numNodes;i++) {
//            LabelledDCNode node = (LabelledDCNode)nodes.get(i);
//            int numStates = (int)node.getNumberOfStates();
//            System.out.print("\n" + node.getName() + ": ");
//            for (int j=0;j<numStates;j++){
//                System.out.print(((LabelledDCNode) node).getBelief(j) + " ");
//            }
//        }
    }



    public static void demo1() throws ExceptionHugin, IOException {


        DynamicBayesianNetwork amidstDBN = DBNExample.getAmidst_DBN_Example();
        //System.out.println(amidstDBN.toString());
        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        String nameModel = "huginDBNFromAMIDST";
        huginDBN.setName(nameModel);
        String outFile = new String("networks/"+nameModel+".net");
        huginDBN.saveAsNet(outFile);
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");




        //************************************************************
        //********************** INFERENCE IN HUGIN ******************
        //************************************************************

        // Create a Domain object with 'nSlices' from a Class object.
        // The dynamic BN is expanded 'nSlices' times.
        Domain domainObject = huginDBN.createDBNDomain(5);

        //InferenceDemo.printCPTs(domainObject);

        //Before entering evidence
        domainObject.compile();
        InferenceDemo.printBeliefs(domainObject);
        domainObject.uncompile();


        LabelledDCNode T0A = (LabelledDCNode)domainObject.getNodeByName("T0.A");

        // Evidence T0.A = 0
        T0A.selectState(0);
        System.out.println("\n\n Evidence entered: " + T0A.evidenceIsEntered());

        //After entering evidence
        domainObject.compile();
        InferenceDemo.printBeliefs(domainObject);
        domainObject.uncompile();









        

    }

    public static void main(String[] args) throws ExceptionHugin, IOException {
        InferenceDemo.demo1();
    }

}
