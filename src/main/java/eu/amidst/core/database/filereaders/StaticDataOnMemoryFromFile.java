package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnMemory;

import java.util.List;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataOnMemoryFromFile extends StaticDataOnDiskFromFile implements DataOnMemory{

    List<StaticDataInstance> dataInstances;

    public StaticDataOnMemoryFromFile(DataFileReader reader_) {
        super(reader_);

        int count=0;
        while(reader.hasMoreDataRows()){
            dataInstances.add(new StaticDataInstance(reader.nextDataRow(),count++));
        }
        reader.reset();
    }

    @Override
    public int getNumberOfDataInstances() {
        return dataInstances.size();
    }

    @Override
    public DataInstance getDataInstance(int i) {
        return dataInstances.get(i);
    }

}
