package eu.amidst.flinklink.examples.io;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by rcabanas on 10/06/16.
 */
public class DataStreamLoaderExample {
    public static void main(String[] args) throws Exception {

        //Set the environment variable
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        //Paths to datasets
        String simpleFile = "datasets/simulated/syntheticData.arff";
        String distriFile = "datasets/simulated/distributed.arff";

        //Load the data
        DataFlink<DataInstance> dataSimple = DataFlinkLoader.open(env, simpleFile, false);
        DataFlink<DataInstance> dataDistri = DataFlinkLoader.open(env,distriFile, false);

        //Print the data
        dataSimple.getDataSet().print();
        dataDistri.getDataSet().print();

    }
}
