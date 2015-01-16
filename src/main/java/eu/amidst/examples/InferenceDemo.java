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



        //************************************************************
        //********************** LEARNING IN AMIDST ******************
        //************************************************************

        String file = "./datasets/bank_data_train.arff";
        DataBase data = new DynamicDataOnDiskFromFile(new ARFFDataReader(file));

        //System.out.println("ATTRIBUTES:");
        //data.getAttributes().getList().stream().forEach(a -> System.out.println(a.getName()));

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 3);//We set -3 to account for time id and seq_id
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork amidstDBN = model.getDynamicBNModel();

        //We randomly initialize the parensets Time 0 because parameters are wrongly learnt due
        //Random rand = new Random(0);
        //amidstDBN.getDistributionsTime0().forEach(w -> w.randomInitialization(rand));
        System.out.println();
        System.out.println();
        System.out.println(amidstDBN.toString());

        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);

         //************************************************************
         //********************** INFERENCE IN HUGIN ******************
         //************************************************************

         file = "./datasets/bank_data_predict.arff";
         data = new DynamicDataOnDiskFromFile(new ARFFDataReader(file));

         // The value of the timeWindow must be sampleSize-1 at maximum
         int timeSlices = 1;


        System.out.println("Computing Probabilities of Defaulting:\n\n");

         Iterator<DataInstance> iterator = data.iterator();
         LabelledDCNode lastDefault =null;

         int currentSequenceID = 0;
         DataInstance dataInstance = iterator.next();

         while (iterator.hasNext()) {
             // Create a DBN runtime domain (from a Class object) with a time window of 'nSlices' .
             // The domain must be created using the method 'createDBNDomain'
             Domain domainObject = huginDBN.createDBNDomain(timeSlices);

             // ENTERING EVIDENCE IN ALL THE SLICES OF THE INITIAL EXPANDED DBN
             for (int i = 0; i <= timeSlices && iterator.hasNext(); i++) {
                 //System.out.println("\n-----> " + dataInstance.getTimeID() + ", " + dataInstance.getSequenceID());

                 for (Variable var: amidstDBN.getDynamicVariables().getListOfDynamicVariables()){
                     //Avoid entering evidence in class variable to have something to predict
                     if ((var.getVarID()!=model.getClassVarID())){
                         LabelledDCNode node = (LabelledDCNode)domainObject.getNodeByName("T"+i+"."+var.getName());
                         node.selectState((long)dataInstance.getValue(var));
                     }
                 }
                 dataInstance= iterator.next();
             }
             lastDefault =  (LabelledDCNode)domainObject.getNodeByName("T"+timeSlices + ".DEFAULT");
             domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
             domainObject.compile();

             while (currentSequenceID==dataInstance.getSequenceID() && iterator.hasNext()) {

                 //System.out.println("TIME_ID: "+ dataInstance.getTimeID() + "  CUSTOMER ID:" +  dataInstance.getSequenceID());

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

                 dataInstance= iterator.next();
             }
             System.out.println("CLIENT ID: " + currentSequenceID + "   " + " Probability of defaulting: " +lastDefault.getBelief(1));
             domainObject.uncompile();

             currentSequenceID = dataInstance.getSequenceID();
         }
     }

    public static void main(String[] args) throws ExceptionHugin, IOException {
        InferenceDemo.demo();
    }

}
