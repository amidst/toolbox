package eu.amidst.huginlink.inference;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.datastream.*;
import eu.amidst.core.io.DynamicDataStreamLoader;
import eu.amidst.core.learning.dynamic.DynamicNaiveBayesClassifier;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.converters.DBNConverterToHugin;

import java.io.IOException;
import java.util.*;

/**
 * This class is a demo for making inference in a Dynamic Bayesian network model learnt from Cajamar data using the
 * Hugin inference engine.
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 13/1/15
 */
public class InferenceDemo {

    /**
     * Prints the belief of all the nodes in the Hugin domain.
     * @param domainObject the expanded dynamic model.
     * @throws ExceptionHugin
     */
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


    /**
     * The demo for the Cajamar case.
     * @throws ExceptionHugin
     * @throws IOException
     */
    public static void demo() throws ExceptionHugin, IOException {

        //************************************************************
        //********************** LEARNING IN AMIDST ******************
        //************************************************************

        String file = "./datasets/bank_data_train.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);

        //System.out.println("ATTRIBUTES:");
        //data.getAttributes().getList().stream().forEach(a -> System.out.println(a.getName()));

        System.out.println("Learning a Dynamic Naive Bayes Classifier.");
        System.out.println("Traning Data: 4000 clients, 1000 days of records for each client, 10 profile variables.");

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 3);//We set -3 to account for time id and seq_id
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork amidstDBN = model.getDynamicBNModel();

        //We randomly initialize the parensets Time 0 because parameters are wrongly learnt due
        //Random rand = new Random(0);
        //amidstDBN.getConditionalDistributionTime0().forEach(w -> w.randomInitialization(rand));
        System.out.println();
        System.out.println();
        System.out.println(amidstDBN.toString());

        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);

         //************************************************************
         //********************** INFERENCE IN HUGIN ******************
         //************************************************************

         file = "./datasets/bank_data_predict.arff";
         data = DynamicDataStreamLoader.loadFromFile(file);

         // The value of the timeWindow must be sampleSize-1 at maximum
         int timeSlices = 9;


         System.out.println("Computing Probabilities of Defaulting for 10 clients using Hugin API:\n");

         Iterator<DynamicDataInstance> iterator = data.iterator();
         LabelledDCNode lastDefault =null;

         int currentSequenceID = 0;
        DynamicDataInstance dataInstance = iterator.next();

        Domain domainObject = huginDBN.createDBNDomain(timeSlices);


         while (iterator.hasNext()) {
             // Create a DBN runtime domain (from a Class object) with a time window of 'nSlices' .
             // The domain must be created using the method 'createDBNDomain'


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

//             while (currentSequenceID==dataInstance.getSequenceID() && iterator.hasNext()) {
//
//                 //System.out.println("TIME_ID: "+ dataInstance.getTimeID() + "  CUSTOMER ID:" +  dataInstance.getSequenceID());
//
//                 //Before moving the window
//                 lastDefault =  (LabelledDCNode)domainObject.getNodeByName("T"+timeSlices + ".DEFAULT");
//                 domainObject.moveDBNWindow(1);
//                 domainObject.uncompile();
//
//                 for (Variable var : amidstDBN.getDynamicVariables().getListOfDynamicVariables()) {
//                     //Avoid entering evidence in class variable to have something to predict
//                     if ((var.getVarID()!=model.getClassVarID())) {
//                         LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T" + timeSlices + "." + var.getName());
//                         node.selectState((long) dataInstance.getValue(var));
//                     }
//                 }
//
//                 domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
//                 domainObject.compile();
//
//                 dataInstance= iterator.next();
//             }
             System.out.println("CLIENT ID: " + currentSequenceID + "   " + " Probability of defaulting: " +lastDefault.getBelief(1));
             domainObject.uncompile();

             domainObject.retractFindings();

             currentSequenceID = dataInstance.getSequenceID();
         }
     }

    public static void main(String[] args) throws ExceptionHugin, IOException {
        InferenceDemo.demo();
    }

}
