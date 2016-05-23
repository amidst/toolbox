package eu.amidst.tutorials.huginsa2016.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.io.FileNotFoundException;

/**
 * Created by rcabanas on 23/05/16.
 */
public class StaticModelFlink {
    public static void main(String[] args) throws FileNotFoundException {

        //Load the datastream
        String filename = "datasets/simulated/exampleDS_d0_c5.arff";
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        DataFlink<DataInstance> data = DataFlinkLoader.loadDataFromFile(env, filename, false);

        //Learn the model
        FactorAnalysis model = new FactorAnalysis(data.getAttributes());
        model.setNumberOfLatentVariables(3);
        model.setWindowSize(200);
        model.updateModel(data);
        BayesianNetwork bn = model.getModel();

        System.out.println(bn);

    }

}
