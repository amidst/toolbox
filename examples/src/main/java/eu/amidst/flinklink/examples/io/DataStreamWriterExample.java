package eu.amidst.flinklink.examples.io;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.utils.DataSetGenerator;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by rcabanas on 09/06/16.
 */
public class DataStreamWriterExample {
    public static void main(String[] args) throws Exception {

        //Set-up Flink session.
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        //generate a random dataset
        DataFlink<DataInstance> dataFlink = new DataSetGenerator().generate(env,1234,1000,2,3);

        //Saves it as a distributed arff file
        DataFlinkWriter.writeDataToARFFFolder(dataFlink, "datasets/simulated/distributed.arff");
    }
}


//TODO: Write to standard arff --> convert to datastream??