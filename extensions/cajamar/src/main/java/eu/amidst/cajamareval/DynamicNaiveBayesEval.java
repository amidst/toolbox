package eu.amidst.cajamareval;

import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
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

        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink =  DataFlinkLoader.loadDynamicDataFromFile(env, fileDay0, true);
        dynamicDataInstanceDataFlink.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));

        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink1 =  DataFlinkLoader.loadDynamicDataFromFile(env, fileDay1, true);
        dynamicDataInstanceDataFlink1.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));



    }
}
