package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnStream;

/**
 *
 */
public class DynamicDataOnStreamFromFile implements DataOnStream {

    DataFileReader reader;
    int sampleID = 0;
    int timeID = 0;
    DataRow present;
    DataRow past;

    public DynamicDataOnStreamFromFile(DataFileReader reader_){
        this.reader=reader_;
    }

    @Override
    public DataInstance nextDataInstance() {
        past = present;
        present = this.reader.nextDataRow();
        return new DynamicDataInstance(present, past, sampleID, timeID);
    }

    @Override
    public boolean hasMoreDataInstances() {
        return reader.hasMoreDataRows();
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }


}
