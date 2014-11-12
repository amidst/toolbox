package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.DataOnStream;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataOnDiskFromFile implements DataOnDisk, DataOnStream{

    DataFileReader reader;

    public StaticDataOnDiskFromFile(DataFileReader reader) {
        this.reader = reader;
    }

    @Override
    public DataInstance nextDataInstance() {
        return new StaticDataInstance(this.reader.nextDataRow());
    }

    @Override
    public boolean hasMoreDataInstances() {
        return reader.hasMoreDataRows();
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public void restart() {
        this.reader.reset();
    }
}
