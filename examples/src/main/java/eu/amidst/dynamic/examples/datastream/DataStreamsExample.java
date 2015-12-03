package eu.amidst.dynamic.examples.datastream;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;

/**
 * An example showing how to load an use a DataStream object. For more options refer to class
 * eu.amidst.core.examples.datastream and simply change DataInstance by DynamicDataInstance
 *
 * Created by ana@cs.aau.dk on 02/12/15.
 */
public class DataStreamsExample {
    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DynamicDataStreamLoader
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile("datasets/dynamicNB-samples.arff");

        //Access to the attributes defining the data set
        System.out.println("Attributes defining the data set");
        for (Attribute attribute : data.getAttributes()) {
            System.out.println(attribute.getName());
        }
        Attribute classVar = data.getAttributes().getAttributeByName("ClassVar");

        //1. Iterating over samples using a for loop
        System.out.println("1. Iterating over samples using a for loop");
        for (DynamicDataInstance dataInstance : data) {
            System.out.println("SequenceID = "+dataInstance.getSequenceID()+", TimeID = "+dataInstance.getTimeID());
            System.out.println("The value of attribute A for the current data instance is: " +
                    dataInstance.getValue(classVar));
        }

    }
}
