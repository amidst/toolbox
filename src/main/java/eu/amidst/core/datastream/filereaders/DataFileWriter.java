package eu.amidst.core.datastream.filereaders;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DataInstance;

import java.io.IOException;

/**
 * Created by andresmasegosa on 23/02/15.
 */
public interface DataFileWriter {

    String getFileExtension();

    void writeToFile(DataStream<? extends DataInstance> dataStream, String file) throws IOException;

}
