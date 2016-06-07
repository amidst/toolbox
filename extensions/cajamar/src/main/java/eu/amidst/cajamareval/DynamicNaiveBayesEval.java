package eu.amidst.cajamareval;

import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.latentvariablemodels.dynamicmodels.classifiers.DynamicNaiveBayesClassifier;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.io.IOException;

/**
 * Created by dario on 6/6/16.
 */
public class DynamicNaiveBayesEval {

    public static void main(String[] args) throws IOException {

        String fileDay0 = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_train/train0.arff";
        String fileDay1 = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_train/train1.arff";

        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink0 = DataFlinkLoader.loadDynamicDataFromFile(env, fileDay0, true);
        dynamicDataInstanceDataFlink0.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));

        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink1 = DataFlinkLoader.loadDynamicDataFromFile(env, fileDay1, true);
        dynamicDataInstanceDataFlink1.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));

        dynamicDataInstanceDataFlink0 = DataFlinkLoader.loadDynamicDataFromFile(env, fileDay0, true);
        dynamicDataInstanceDataFlink1 = DataFlinkLoader.loadDynamicDataFromFile(env, fileDay1, true);

        DynamicNaiveBayesClassifier dynamicNaiveBayesClassifier = new DynamicNaiveBayesClassifier(dynamicDataInstanceDataFlink0.getAttributes());

        dynamicNaiveBayesClassifier.setClassName("Default");
        dynamicNaiveBayesClassifier.setConnectChildrenTemporally(true);

        System.out.println(dynamicNaiveBayesClassifier.getDynamicDAG());

        dynamicNaiveBayesClassifier.updateModel(0,dynamicDataInstanceDataFlink0);
        dynamicNaiveBayesClassifier.updateModel(1,dynamicDataInstanceDataFlink1);
        System.out.println(dynamicNaiveBayesClassifier.getModel());






//        DataStream<DynamicDataInstance> dataInstanceDataStream0 = DynamicDataStreamLoader.loadFromFile(fileDay0);
//        dataInstanceDataStream0.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));
//
//        System.out.println();
//
//        DataStream<DynamicDataInstance> dataInstanceDataStream1 = DynamicDataStreamLoader.loadFromFile(fileDay1);
//        dataInstanceDataStream1.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));
//
//        System.out.println();
//
//        DynamicNaiveBayesClassifier dynamicNaiveBayesClassifier = new DynamicNaiveBayesClassifier(dataInstanceDataStream0.getAttributes());
//
//        dynamicNaiveBayesClassifier.setClassName("Default");
//        dynamicNaiveBayesClassifier.setConnectChildrenTemporally(true);
//
//        dynamicNaiveBayesClassifier.updateModel(dataInstanceDataStream0);
//        System.out.println(dynamicNaiveBayesClassifier.getModel());
//
//        System.out.println("Test data with # instances: " + dataInstanceDataStream1.stream().count());
//
//
//        Variables variables = new Variables(dataInstanceDataStream1.getAttributes());
//
//        dataInstanceDataStream1 = DynamicDataStreamLoader.loadFromFile(fileDay1);
//
//        for(DynamicDataInstance dynamicDataInstance : dataInstanceDataStream1) {
//            System.out.println(dynamicDataInstance);
//        }
//        List<DynamicDataInstance> dataTrain = dataInstanceDataStream0.stream().collect(Collectors.toList());
//        List<DynamicDataInstance> dataTest = dataInstanceDataStream1.stream().collect(Collectors.toList());




//        System.out.println("TRAIN DATA INSTANCES:");
//        for (int i = 0; i < 10; i++) {
//            DynamicDataInstance dynamicDataInstance = dataTrain.get(i);
//            System.out.println("SEQUENCE_ID = " + dynamicDataInstance.getSequenceID() + ", TIME_ID = " + dynamicDataInstance.getTimeID() + ", " + dynamicDataInstance.outputString(variables.getListOfVariables()));
//
//        }
//
//        System.out.println("TEST DATA INSTANCES:");
//        for (int i = 0; i < 10; i++) {
//            DynamicDataInstance dynamicDataInstance = dataTest.get(i);
//            System.out.println(dynamicDataInstance.outputString());
//            //System.out.println("SEQUENCE_ID = " + dynamicDataInstance.getSequenceID() + ", TIME_ID = " + dynamicDataInstance.getTimeID() + ", " + dynamicDataInstance.outputString(variables.getListOfVariables()));
//
//        }

//        for(DynamicDataInstance dynamicDataInstance : dataTest) {
//
//            dynamicDataInstance.setValue(dynamicNaiveBayesClassifier.getClassVar(), Utils.missingValue());
//            Multinomial classVarPosteriorDistribution = dynamicNaiveBayesClassifier.predict(dynamicDataInstance);
//            System.out.println(classVarPosteriorDistribution.toString());
//            dynamicNaiveBayesClassifier.resetModel();
//        }
    }
}
