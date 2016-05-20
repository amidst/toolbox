package eu.amidst.icdm2016;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;

import java.io.IOException;

/**
 * Created by ana@cs.aau.dk on 19/04/16.
 */
public class DistributeDataInMonths {
    public static void main(String[] args) throws IOException {
        DataStream<DataInstance> data = DataStreamLoader.openFromFile("/Users/ana/Documents/Amidst-MyFiles/CajaMar/" +
                "dataWekaUnemploymentRate.arff");

        Attributes attributes = data.getAttributes();
        Attribute timeID = attributes.getTime_id();

        for (int i = 0; i < 84; i++) {
            int index = i;
            DataStreamWriter.writeDataToFile(data.filter(dataInstance -> dataInstance.getValue(timeID)==index),"/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWekaUnemploymentRate/" +
                    "dataWekaUnemploymentRate"+index+".arff");
            data.restart();
        }
    }
}
