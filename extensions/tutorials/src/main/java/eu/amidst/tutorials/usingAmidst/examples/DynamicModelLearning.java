package eu.amidst.tutorials.usingAmidst.examples;



import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.latentvariablemodels.dynamicmodels.DynamicModel;
import eu.amidst.latentvariablemodels.dynamicmodels.HiddenMarkovModel;

/**
 * Created by rcabanas on 23/05/16.
 */
public class DynamicModelLearning {
    public static void main(String[] args) {

        //Load the datastream
        String filename = "datasets/simulated/exampleDS_d0_c5.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(filename);

        //Learn the model
        DynamicModel model = new HiddenMarkovModel(data.getAttributes());
        model.updateModel(data);
        DynamicBayesianNetwork dbn = model.getModel();


        System.out.println(dbn);

    }
}
