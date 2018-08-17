package eu.amidst.tutorial.usingAmidst.examples;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;
import eu.amidst.latentvariablemodels.staticmodels.Model;
import eu.amidst.tutorial.usingAmidst.Main;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.io.IOException;

/**
 * Created by rcabanas on 23/05/16.
 */
public class StaticModelFlink {
    public static void main(String[] args) throws IOException, ExceptionHugin {


        //Set-up Flink session.
       // Configuration conf = new Configuration();
      //  conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        //env.getConfig().disableSysoutLogging();
       // env.setParallelism(Main.PARALLELISM);

        //Load the datastream
        String filename = "datasets/simulated/cajamarDistributed.arff";
        DataFlink<DataInstance> data = DataFlinkLoader.open(env, filename, false);

        //Learn the model
        Model model = new FactorAnalysis(data.getAttributes());
        model.updateModel(data);
        BayesianNetwork bn = model.getModel();

        System.out.println(bn);

        // Save with .bn format
        BayesianNetworkWriter.save(bn, "networks/simulated/exampleBN.bn");

        // Save with hugin format
        //BayesianNetworkWriterToHugin.save(bn, "networks/simulated/exampleBN.net");

    }

}
