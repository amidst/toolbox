package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.DataOnDisk;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataOnDiskFromFile extends StaticDataOnStreamFromFile implements DataOnDisk{

    public StaticDataOnDiskFromFile(DataFileReader reader_) {
        super(reader_);
    }

    @Override
    public void restart() {
        this.reader.reset();
    }
}
