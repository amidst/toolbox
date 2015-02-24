package eu.amidst.core.io;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.datastream.filereaders.DataFileReader;
import eu.amidst.core.datastream.filereaders.DynamicDataStreamFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;

/**
 * Created by andresmasegosa on 23/02/15.
 */
public class DynamicDataStreamLoader {

    private static DataFileReader dataFileReader = new ARFFDataReader();


    public static void setDataFileReader(DataFileReader dataFileReader) {
        dataFileReader = dataFileReader;
    }

    public static DataStream<DynamicDataInstance> loadFromFile(String path){
        dataFileReader.loadFromFile(path);
        return new DynamicDataStreamFromFile(dataFileReader);
    }

}
