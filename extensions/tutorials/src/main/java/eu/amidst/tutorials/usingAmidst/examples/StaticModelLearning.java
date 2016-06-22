package eu.amidst.tutorials.usingAmidst.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;
import eu.amidst.latentvariablemodels.staticmodels.Model;

/**
 * Created by rcabanas on 23/05/16.
 */
public class StaticModelLearning {

    public static void main(String[] args) {

        //Load the datastream
        String filename = "datasets/simulated/exampleDS_d0_c5.arff";
        DataStream<DataInstance> data = DataStreamLoader.openFromFile(filename);

        //Learn the model
        Model model = new FactorAnalysis(data.getAttributes());
        model.updateModel(data);
        BayesianNetwork bn = model.getModel();

        System.out.println(bn);


    }

}
