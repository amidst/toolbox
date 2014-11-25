package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.DataOnStream;

import java.util.Iterator;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataOnDiskFromFile implements DataOnDisk, DataOnStream{

    DataFileReader reader;

    public StaticDataOnDiskFromFile(DataFileReader reader) {
        this.reader = reader;
    }

    @Override
    public DataInstance next() {
        return new StaticDataInstance(this.reader.next());
    }

    @Override
    public boolean hasNext() {
        return reader.hasNext();
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public void restart() {
        this.reader.reset();
    }

    @Override
    public Iterator<DataInstance> iterator() {
        return this;
    }
}
