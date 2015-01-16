package eu.amidst.examples;


import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.huginlink.*;
import eu.amidst.core.learning.DynamicNaiveBayesClassifier;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.*;


/**
 * Created by afa on 13/1/15.
 */
public class InferenceDemo {

    public static void printBeliefs (Domain domainObject) throws ExceptionHugin {

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



    public static void demo() throws ExceptionHugin, IOException {

        //Generate random data
//        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
//        BayesianNetworkGenerator.setNumberOfDiscreteVars(3);
//        BayesianNetworkGenerator.setNumberOfStates(2);
//        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(new Random(0),2);
//        int sampleSize = 20;
//        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
//        sampler.setParallelMode(false);
//        String file = "./datasets/randomdata.arff";
//        sampler.sampleToAnARFFFile(file,sampleSize);

        //String file = "./datasets/bank_data_small.arff";
        String file = "./datasets/bank_data.arff";
        //String file = "./datasets/randomdata2.arff";

        DataBase data = new DynamicDataOnDiskFromFile(new ARFFDataReader(file));

        System.out.println("ATTRIBUTES:");
        data.getAttributes().getList().stream().forEach(a -> System.out.println(a.getName()));



        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();

        // -3 instead of -1 because we have to ignore TIME_ID and SEQUENCE_ID as they are not variables in the model.
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 3);
        model.setParallelMode(true);

        model.learn(data);
        DynamicBayesianNetwork amidstDBN = model.getDynamicBNModel();

        //We randomly initialize the parensets Time 0 because parameters are wrongly learnt due
        //to the data sample only contains 1 data sequence.
        Random rand = new Random(0);
        amidstDBN.getDistributionsTime0().forEach(w -> w.randomInitialization(rand));


        System.out.println(amidstDBN.toString());

        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        String nameModel = "CajamarDBN";
        huginDBN.setName(nameModel);
        String outFile = new String("networks/" + nameModel + ".net");
        huginDBN.saveAsNet(outFile);
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");


         //************************************************************
         //********************** INFERENCE IN HUGIN ******************
         //************************************************************

         // The value of the timeWindow must be sampleSize-1 at maximum
         int timeSlices = 5;

         // Create a DBN runtime domain (from a Class object) with a time window of 'nSlices' .
         // The domain must be created using the method 'createDBNDomain'
         Domain domainObject = huginDBN.createDBNDomain(timeSlices);

        nameModel = "CajamarDBNExpanded";
        huginDBN.setName(nameModel);
        outFile = new String("networks/" + nameModel + ".net");
        domainObject.saveAsNet(outFile);

         //Beliefs before entering evidence
         System.out.println("\n\nBELIEFS before propagating evidence: ");
         domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
         domainObject.compile();
         InferenceDemo.printBeliefs(domainObject);
         domainObject.uncompile();


        // ENTERING EVIDENCE IN ALL THE SLICES OF THE INITIAL EXPANDED DBN
         Iterator<DataInstance> iterator = data.iterator();
         for (int i = 0; i <= timeSlices && iterator.hasNext(); i++) {

             DataInstance dataInstance= iterator.next();

             System.out.println(dataInstance.getTimeID() + ", " +dataInstance.getSequenceID());

             for (Variable var: amidstDBN.getDynamicVariables().getListOfDynamicVariables()){
                 //Avoid entering evidence in class variable to have something to predict
                 if ((var.getVarID()!=model.getClassVarID())){
                     LabelledDCNode node = (LabelledDCNode)domainObject.getNodeByName("T"+i+"."+var.getName());
                     node.selectState((long)dataInstance.getValue(var));
                 }
             }
         }

         // Beliefs after propagating the evidence
         System.out.println("\n----------------------------------------------------");
         System.out.println("\n\nBELIEFS after propagating evidence: ");
         domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
         domainObject.compile();


        LabelledDCNode lastDefault =null;

        // InferenceDemo.printBeliefs(domainObject);

        int currentSequenceID = 1;

         while (iterator.hasNext()) {

             DataInstance dataInstance = iterator.next();

             while (currentSequenceID==dataInstance.getSequenceID()) {

                 System.out.println("TIME_ID: "+ dataInstance.getTimeID() + "  CUSTOMER ID:" +  dataInstance.getSequenceID());

                 dataInstance = iterator.next();

                 //Before moving the window
                 lastDefault =  (LabelledDCNode)domainObject.getNodeByName("T"+timeSlices + ".DEFAULT");

                 domainObject.moveDBNWindow(1);
                 domainObject.uncompile();

                 for (Variable var : amidstDBN.getDynamicVariables().getListOfDynamicVariables()) {

                     //Avoid entering evidence in class variable to have something to predict
                     if ((var.getVarID()!=model.getClassVarID())) {
                         LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T" + timeSlices + "." + var.getName());
                         node.selectState((long) dataInstance.getValue(var));
                     }
                 }

                 domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
                 domainObject.compile();



             }
             currentSequenceID = dataInstance.getSequenceID();

             System.out.println("NEW CLIENT");


             System.out.println(lastDefault.getName());
             double probDefaulter = lastDefault.getBelief(1);

             System.out.println("CLIENT ID: " + (dataInstance.getSequenceID()-1) + "   " + " Probability of defaulting:" +probDefaulter);

         }
     }

    public static void main(String[] args) throws ExceptionHugin, IOException {
        InferenceDemo.demo();
    }

}
