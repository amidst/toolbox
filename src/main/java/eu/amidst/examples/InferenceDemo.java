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

        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(3);
        BayesianNetworkGenerator.setNumberOfStates(2);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(new Random(0),2);


        int sampleSize = 20;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setParallelMode(false);
        String file = "./datasets/randomdata.arff";
        sampler.sampleToAnARFFFile(file,sampleSize);

        DataBase data = new DynamicDataOnDiskFromFile(new ARFFDataReader(file));

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork amidstDBN = model.getDynamicBNModel();

        //We randomly initialize the parensets Time 0 because parameters are wrongly learnt due
        //to the data sample only contains 1 data sequence.
        Random rand = new Random(0);
        amidstDBN.getDistributionsTime0().forEach(w -> w.randomInitialization(rand));

        System.out.println(amidstDBN.toString());

        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        String nameModel = "huginDBNFromAMIDST";
        huginDBN.setName(nameModel);
        String outFile = new String("networks/"+nameModel+".net");
        huginDBN.saveAsNet(outFile);
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");


        //************************************************************
        //********************** INFERENCE IN HUGIN ******************
        //************************************************************

        // The value of the timeWindow must be sampleSize-1 at maximum
        int timeWindow = 4;

        // Create a DBN runtime domain (from a Class object) with a time window of 'nSlices' .
        // The domain must be created using the method 'createDBNDomain'
        Domain domainObject = huginDBN.createDBNDomain(timeWindow);



        //Beliefs before entering evidence
        System.out.println("\n\nBELIEFS before propagating evidence: ");
        domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
        domainObject.compile();
        InferenceDemo.printBeliefs(domainObject);
        domainObject.uncompile();


        Iterator<DataInstance> iterator = data.iterator();

        for (int i = 0; i <= timeWindow && iterator.hasNext(); i++) {
            DataInstance dataInstance= iterator.next();

            for (Variable var: amidstDBN.getDynamicVariables().getListOfDynamicVariables()){

                //Avoid entering evidence in class variable to have something to predict
                if ((var.getName().compareTo("ClassVar")!=0)){
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
        InferenceDemo.printBeliefs(domainObject);


        while (iterator.hasNext()) {
            System.out.println("\n----------------------------------------------------");
            System.out.println("\nMoving the windows 1 step forward");
            domainObject.moveDBNWindow(1);
            domainObject.uncompile();

            DataInstance dataInstance = iterator.next();

            for (Variable var : amidstDBN.getDynamicVariables().getListOfDynamicVariables()) {
                LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T" + timeWindow + "." + var.getName());
                node.selectState((long) dataInstance.getValue(var));
            }

            System.out.println("BELIEFS after propagating evidence: ");
            domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
            domainObject.compile();
            // domainObject.computeDBNPredictions(3);
            InferenceDemo.printBeliefs(domainObject);
            //System.out.println("CUSTOMER ID: " + "Probability of defaulting:" +
            //        ((LabelledDCNode)domainObject.getNodeByName("T180.ClassVar")).getBelief(0));



        }

    }

    public static void main(String[] args) throws ExceptionHugin, IOException {
        InferenceDemo.demo();
    }

}
