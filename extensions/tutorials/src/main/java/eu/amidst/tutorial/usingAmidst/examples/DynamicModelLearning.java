package eu.amidst.tutorial.usingAmidst.examples;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.latentvariablemodels.dynamicmodels.DynamicModel;
import eu.amidst.latentvariablemodels.dynamicmodels.HiddenMarkovModel;

import java.io.IOException;

/**
 * Created by rcabanas on 23/05/16.
 */
public class DynamicModelLearning {

    public static void main(String[] args) throws IOException, ExceptionHugin {

        //Load the datastream
        String filename = "datasets/simulated/cajamar.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(filename);


        //Learn the model
        DynamicModel model = new HiddenMarkovModel(data.getAttributes());
        model.updateModel(data);
        DynamicBayesianNetwork dbn = model.getModel();


        System.out.println(dbn);


        // Save with .bn format
        DynamicBayesianNetworkWriter.save(dbn, "networks/simulated/exampleDBN.dbn");

        // Save with hugin format
        //DynamicBayesianNetworkWriterToHugin.save(dbn, "networks/simulated/exampleDBN.net");

    }


}
