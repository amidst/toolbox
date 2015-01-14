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

    public static void printCPTs (Class huginDBN) throws ExceptionHugin {

        System.out.println("-------------------------------");
        System.out.println("CONDITIONAL PROBABILITY TABLES:");
        System.out.println("-------------------------------");

        huginDBN.getNodes().stream().forEach((node) -> {
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

        // CPTs of the DBN
        InferenceDemo.printCPTs(huginDBN);

        // Create a DBN runtime domain (from a Class object) with a time window of 'nSlices' .
        // The domain must be created using the method 'createDBNDomain'
        Domain domainObject = huginDBN.createDBNDomain(3);



        //Beliefs before entering evidence
        domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
        domainObject.compile();
        InferenceDemo.printBeliefs(domainObject);
        domainObject.uncompile();


        // Entering a discrete evidence T0.A = 0
        LabelledDCNode T2A = (LabelledDCNode)domainObject.getNodeByName("T2.A");
        T2A.selectState(0);
        System.out.println("\n\n Evidence entered: " + T2A.evidenceIsEntered());

        // Beliefs after propagating the evidence
        domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
        domainObject.compile();
        InferenceDemo.printBeliefs(domainObject);



        // Move the windows n steps forward
        domainObject.moveDBNWindow(1);
        domainObject.uncompile();
        domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
        domainObject.compile();
       // domainObject.computeDBNPredictions(1);
        InferenceDemo.printBeliefs(domainObject);



        // Move the windows n steps forward
        domainObject.moveDBNWindow(1);
        domainObject.uncompile();
        domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
        domainObject.compile();
        // domainObject.computeDBNPredictions(1);
        InferenceDemo.printBeliefs(domainObject);

        //domainObject.computeDBNPredictions(1);

        //WindowOffset: Number of times that the windows of domain has been moved
        //System.out.println(domainObject.getDBNWindowOffset());



    }

    public static void main(String[] args) throws ExceptionHugin, IOException {
        InferenceDemo.demo1();
    }

}
