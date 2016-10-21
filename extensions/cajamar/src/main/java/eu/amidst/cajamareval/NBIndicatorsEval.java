package eu.amidst.cajamareval;

import COM.hugin.HAPI.Domain;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.utils.Utils;
import eu.amidst.huginlink.converters.BNConverterToHugin;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.NaiveBayesClassifier;

import java.io.FileWriter;

/**
 * Created by dario on 21/10/16.
 */
public class NBIndicatorsEval {


    public static void main(String[] args) throws Exception {

        String fileTrain = args[0];
        String fileTest = args[1];
        String fileOutput = args[2];
        String className = args[3];


        DataStream<DataInstance> train = DataStreamLoader.openFromFile(fileTrain);
        DataStream<DataInstance> test = DataStreamLoader.openFromFile(fileTest);
        FileWriter fw = new FileWriter(fileOutput);


        NaiveBayesClassifier naiveBayesClassifier = new NaiveBayesClassifier(train.getAttributes());

        naiveBayesClassifier.setClassName(className);
        naiveBayesClassifier.setWindowSize(10000);
        naiveBayesClassifier.updateModel(train);

        BayesianNetworkWriter.save(naiveBayesClassifier.getModel(), fileOutput + "_NB_model.bn");

        Domain huginNetwork = BNConverterToHugin.convertToHugin(naiveBayesClassifier.getModel());
        huginNetwork.saveAsNet(fileOutput + "_NB_model.net");

        System.out.println(naiveBayesClassifier.getModel());

        Attribute seq_id = train.getAttributes().getSeq_id();
        Attribute classAtt  = train.getAttributes().getAttributeByName(className);
        for (DataInstance dataInstance : test) {
            dataInstance.setValue(classAtt, Utils.missingValue());
            fw.write(dataInstance.getValue(seq_id) + "\t" + naiveBayesClassifier.predict(dataInstance).getParameters()[1] + "\n");
        }

        fw.close();

    }
}
