package eu.amidst.core.examples.io;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;

/**
 *
 * In this example we show how to load and save data sets from ".arff" files (http://www.cs.waikato.ac.nz/ml/weka/arff.html)
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class DataStreamIOExample {

    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.open("datasets/simulated/syntheticData.arff");

        //We can save this data set to a new file using the static class DataStreamWriter
        DataStreamWriter.writeDataToFile(data, "datasets/simulated/tmp.arff");



    }
}
