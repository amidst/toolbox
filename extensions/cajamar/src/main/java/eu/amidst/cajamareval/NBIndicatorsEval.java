package eu.amidst.cajamareval;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Created by dario on 21/10/16.
 */
public class NBIndicatorsEval {


    public static void main(String[] args) throws Exception {

        String className = "Default";

        String fileTrain;
        String fileTest;
        String outputFolder;
        String dataSetName;

        if(args.length == 4) {
            fileTrain = args[0];
            fileTest = args[1];
            outputFolder = args[2];
            dataSetName = args[3];
        }
        else {

            System.out.println("Incorrect number of arguments, use: \"NBIndicatorsEval fileTrain fileTest outputFolder dataSetName \"");

            //String folder = "/Users/dario/Desktop/CAJAMAR_Estaticos/10-11-2016_reales/";
            String folder = "/Users/dario/Desktop/CAJAMAR_Indicators/";
            fileTrain  =  folder + "train.arff";  //CAJAMAR_DatosNB
            fileTest   =  folder + "test.arff";
            outputFolder =  folder;
            dataSetName = "";
        }

        String fileOutput   =   outputFolder + "NBIndicators_" + dataSetName + "_predictions.csv";
        String modelOutput  =   outputFolder + "NBIndicators_" + dataSetName + "_model.bn";
        String modelOutputTxt = outputFolder + "NBIndicators_" + dataSetName + "_model.txt";

        DataStream<DataInstance> train = DataStreamLoader.open(fileTrain);
        DataStream<DataInstance> test = DataStreamLoader.open(fileTest);
        FileWriter fw = new FileWriter(fileOutput);


        NBIndicatorsClassifier nbIndicatorsClassifier = new NBIndicatorsClassifier(train.getAttributes());

        nbIndicatorsClassifier.setClassName(className);
        nbIndicatorsClassifier.setWindowSize(5000);
        nbIndicatorsClassifier.updateModel(train);

        BayesianNetworkWriter.save(nbIndicatorsClassifier.getModel(), modelOutput);

        File modelOutputFile = new File(modelOutputTxt);
        PrintWriter modelWriter = new PrintWriter(modelOutputFile, "UTF-8");
        modelWriter.print(nbIndicatorsClassifier.getModel().toString());
        modelWriter.close();


        //Domain huginNetwork = BNConverterToHugin.convertToHugin(nbIndicatorsClassifier.getModel());
        //huginNetwork.saveAsNet(fileOutput + "_NB_model.net");

        System.out.println(nbIndicatorsClassifier.getModel());

        Attribute seq_id = train.getAttributes().getSeq_id();
        Attribute classAtt  = train.getAttributes().getAttributeByName(className);
        for (DataInstance dataInstance : test) {
            double actualClass = dataInstance.getValue(classAtt);
            dataInstance.setValue(classAtt, Utils.missingValue());
            fw.write((long)dataInstance.getValue(seq_id) + "," + nbIndicatorsClassifier.predict(dataInstance).getParameters()[1] + "," + (long)actualClass + "\n");
        }

        fw.close();

    }
}
