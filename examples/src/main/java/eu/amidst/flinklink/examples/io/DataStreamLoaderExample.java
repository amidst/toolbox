package eu.amidst.flinklink.examples.io;

import eu.amidst.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

/**
 * Created by rcabanas on 10/06/16.
 */
public class DataStreamLoaderExample {
    public static void main(String[] args) throws Exception {

        boolean hadoop_cluster = false;

        if (args.length>1){
            hadoop_cluster = Boolean.parseBoolean(args[0]);
        }

        final ExecutionEnvironment env;

        //Set-up Flink session.
        if(hadoop_cluster){
            env = ExecutionEnvironment.getExecutionEnvironment();
            env.getConfig().disableSysoutLogging();
        }else{
            Configuration conf = new Configuration();
            conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
            conf.setInteger("taskmanager.numberOfTaskSlots",Main.PARALLELISM);
            env = ExecutionEnvironment.createLocalEnvironment(conf);
            env.setParallelism(Main.PARALLELISM);
            env.getConfig().disableSysoutLogging();
        }
        //Paths to datasets
        String simpleFile = "datasets/simulated/syntheticData.arff";
        String distriFile = "datasets/simulated/distributed.arff";

        //Load the data
        DataFlink<DataInstance> dataSimple = DataFlinkLoader.open(env, simpleFile, false);
        DataFlink<DataInstance> dataDistri = DataFlinkLoader.open(env,distriFile, false);

        //Print the number of data samples
        System.out.println(dataSimple.getDataSet().count());
        System.out.println(dataDistri.getDataSet().count());

    }
}
