package eu.amidst.cajamareval;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.utils.Utils;

import java.io.FileWriter;

/**
 * Created by dario on 21/10/16.
 */
public class NBIndicatorsEval {


    public static void main(String[] args) throws Exception {


        String fileTrain;
        String fileTest;
        String fileOutput;
        String className;

        if(args.length == 4) {
            fileTrain = args[0];
            fileTest = args[1];
            fileOutput = args[2];
            className = args[3];
        }
        else {
            fileTrain = "/Users/dario/Desktop/Datos21-10-2016/train.arff";  //CAJAMAR_DatosNB
            fileTest = "/Users/dario/Desktop/Datos21-10-2016/test.arff";
            fileOutput = "/Users/dario/Desktop/Datos21-10-2016/output.txt";
            className = "Default";
        }

        DataStream<DataInstance> train = DataStreamLoader.open(fileTrain);
        DataStream<DataInstance> test = DataStreamLoader.open(fileTest);
        FileWriter fw = new FileWriter(fileOutput);


        NBIndicatorsClassifier nbIndicatorsClassifier = new NBIndicatorsClassifier(train.getAttributes());

        nbIndicatorsClassifier.setClassName(className);
        nbIndicatorsClassifier.setWindowSize(10000);
        nbIndicatorsClassifier.updateModel(train);

        BayesianNetworkWriter.save(nbIndicatorsClassifier.getModel(), fileOutput + "_NBIndicators_model.bn");

        //Domain huginNetwork = BNConverterToHugin.convertToHugin(nbIndicatorsClassifier.getModel());
        //huginNetwork.saveAsNet(fileOutput + "_NB_model.net");

        System.out.println(nbIndicatorsClassifier.getModel());

        Attribute seq_id = train.getAttributes().getSeq_id();
        Attribute classAtt  = train.getAttributes().getAttributeByName(className);
        for (DataInstance dataInstance : test) {
            dataInstance.setValue(classAtt, Utils.missingValue());
            fw.write(dataInstance.getValue(seq_id) + "\t" + nbIndicatorsClassifier.predict(dataInstance).getParameters()[1] + "\n");
        }

        fw.close();

    }
}
