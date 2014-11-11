package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnStream;
import eu.amidst.core.database.Attributes;

/**
 *
 */
public class StaticDataOnStreamFromFile implements DataOnStream {

    DataFileReader reader;
    int sampleID = 0;

    public StaticDataOnStreamFromFile(DataFileReader reader_){
        this.reader=reader_;
    }

    @Override
    public DataInstance nextDataInstance() {
        return new StaticDataInstance(this.reader.nextDataRow(),this.sampleID);
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
