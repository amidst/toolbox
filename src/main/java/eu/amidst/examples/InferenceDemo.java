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
import java.util.Iterator;
import java.util.List;


/**
 * Created by afa on 13/1/15.
 */
public class InferenceDemo {

    public static void demo1() throws ExceptionHugin, IOException {


        DynamicBayesianNetwork amidstDBN = DBNExample.getAmidst_DBN_Example();
        System.out.println(amidstDBN.toString());
        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        String nameModel = "huginDBNFromAMIDST";
        huginDBN.setName(nameModel);
        String outFile = new String("networks/"+nameModel+".net");
        huginDBN.saveAsNet(outFile);
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");





        //Inference in Hugin
        //Expand the DBN 5 time slices to make inference
        Domain domainObject = new Domain(huginDBN,3);
        domainObject.resetInferenceEngine();




        
        //domainObject.simulate();
        domainObject.newCase();
        domainObject.enterCase(0);
        domainObject.compile();



        System.out.println(domainObject.getNumberOfCases());


        ParseListener parseListener = new DefaultClassParseListener();
        //domainObject.parseCase(,parseListener);

        //DataSet data = new DataSet();
        //data.setDataItem(1,1,"HOLA");
        //data.saveAsCSV("datasets/dynamicData.csv", 'a');

        //domainObject.addCases(data);

//        domainObject.setNumberOfCases(1);
  //      System.out.println("\n Number of cases: " + domainObject.getNumberOfCases());

//
//        ARFFDataReader reader = new ARFFDataReader("datasets/dataWeka/laborTimeIDSeqID.arff");
//        Attributes attributes = reader.getAttributes();
//        DataOnDisk dataOnDisk = new DynamicDataOnDiskFromFile(reader);
//        Iterator<DataInstance> dataOnDiskIterator = dataOnDisk.iterator();
//        DynamicVariables dynamicVariables = new DynamicVariables(attributes);
//        List<Variable> obsVars = dynamicVariables.getListOfDynamicVariables();
//        //temporalClones = dynamicVariables.getListOfTemporalClones();
//
//        domainObject.parseCase("dataset/huginNetworkFromAMIDST.net", parseListener);
//
//        System.out.println(domainObject.newCase());


       // domainObject.propagate();



       //
       // domainObject.triangulate();
       // domainObject.compile();
//

        //domainObject.computeDBNPredictions(3);

//        domainObject.propagate();



  //      domainObject.moveDBNWindow(1);

        //Enter a case as evidence
//        domainObject.enterCase(2);



        //Tests if evidence has been entered since the last propagation.
   //     domainObject.evidenceIsPropagated();

        

    }

    public static void main(String[] args) throws ExceptionHugin, IOException {
        InferenceDemo.demo1();
    }

}
