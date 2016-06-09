package eu.amidst.cajamareval;

import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by dario on 6/6/16.
 */
public class DynamicNaiveBayesAggregatedEval {

    public static void main(String[] args) throws Exception {


        String fileDay0 = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_train/train0.arff";
        String fileDay1 = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_train/train1.arff";

        String fileTest0 = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_test/test.arff";


        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink0 = DataFlinkLoader.loadDynamicDataFromFolder(env, fileDay0, false);
        dynamicDataInstanceDataFlink0.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));

        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink1 = DataFlinkLoader.loadDynamicDataFromFolder(env, fileDay1, false);


        DynamicNaiveBayesWithAggregatedClassifier dynamicNaiveBayesClassifier = new DynamicNaiveBayesWithAggregatedClassifier(dynamicDataInstanceDataFlink0.getAttributes());

        dynamicNaiveBayesClassifier.setClassName("Default");
        dynamicNaiveBayesClassifier.setConnectChildrenTemporally(true);

        System.out.println(dynamicNaiveBayesClassifier.getDynamicDAG());
//
//        dynamicNaiveBayesClassifier.updateModel(0,dynamicDataInstanceDataFlink0);
//
//        System.out.println("\n\nUPDATED WITH TIME 0\n\n");
//        dynamicNaiveBayesClassifier.updateModel(1,dynamicDataInstanceDataFlink1);
//
//        System.out.println("\n\nUPDATED WITH TIME 1\n\n");
//        System.out.println(dynamicNaiveBayesClassifier.getModel());
//
//        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlinkTest = DataFlinkLoader.loadDynamicDataFromFolder(env, fileTest0, false);
//
//
//        List<DynamicDataInstance> dataTest = dynamicDataInstanceDataFlinkTest.getDataSet().collect();
//
//        for(DynamicDataInstance dynamicDataInstance : dataTest) {
//            double classValue = dynamicDataInstance.getValue(dynamicNaiveBayesClassifier.getClassVar());
//            dynamicDataInstance.setValue(dynamicNaiveBayesClassifier.getClassVar(), Utils.missingValue());
//            Multinomial classVarPosteriorDistribution = dynamicNaiveBayesClassifier.predict(dynamicDataInstance);
//            System.out.println("Class value: " + classValue + ", predicted prob. of defaulting: " + classVarPosteriorDistribution.getParameters()[1] );
//        }

    }
}
