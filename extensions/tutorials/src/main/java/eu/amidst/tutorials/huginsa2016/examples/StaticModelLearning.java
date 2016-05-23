package eu.amidst.tutorials.huginsa2016.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;

/**
 * Created by rcabanas on 23/05/16.
 */
public class StaticModelLearning {

    public static void main(String[] args) {

        //Load the datastream
        String filename = "datasets/simulated/exampleDS_d0_c5.arff";
        DataStream<DataInstance> data = DataStreamLoader.openFromFile(filename);

        //Learn the model
        FactorAnalysis model = new FactorAnalysis(data.getAttributes());
        model.setNumberOfLatentVariables(3);
        model.setWindowSize(200);
        model.updateModel(data);
        BayesianNetwork bn = model.getModel();

        System.out.println(bn);


    }

}
