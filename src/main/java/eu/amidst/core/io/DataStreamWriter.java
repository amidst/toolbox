package eu.amidst.core.io;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.DataFileReader;
import eu.amidst.core.datastream.filereaders.DataFileWriter;
import eu.amidst.core.datastream.filereaders.DataStreamFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;

import java.io.IOException;

/**
 * Created by andresmasegosa on 07/04/15.
 */
public final class DataStreamWriter {

    private static DataFileWriter dataFileWriter = new ARFFDataWriter();


    public static void setDataFileWriter(DataFileWriter dataFileWriter_) {
        dataFileWriter = dataFileWriter_;
    }

    public static void writeDataToFile(DataStream<? extends DataInstance> data, String path) throws IOException {
        dataFileWriter.writeToFile(data, path);
    }

}
