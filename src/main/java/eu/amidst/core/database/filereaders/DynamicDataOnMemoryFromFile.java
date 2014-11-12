package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 12/11/14.
 */
public class DynamicDataOnMemoryFromFile implements DataOnMemory, DataOnDisk, DataOnStream {

    DataFileReader reader;
    /**
     * Only used in case the sequenceID is not in the datafile
     */
    int sequenceID = 0;
    /**
     * Only used in case the timeID is not in the datafile, time ID of the Present.
     */
    int timeIDcounter = 0;
    DataRow present;
    DataRow past;
    Attribute attSequenceID;
    Attribute attTimeID;
    DynamicDataInstance[] dataInstances;
    int pointer;


    public DynamicDataOnMemoryFromFile(DataFileReader reader) {
        this.reader = reader;

        List<DynamicDataInstance> dataInstancesList = new ArrayList<>();

        while (reader.hasMoreDataRows()) {
            //dataInstancesList.add(new DynamicDataInstance(reader.nextDataRow(),reader.nextDataRow()));
        }
        reader.reset();

        dataInstances = new DynamicDataInstance[dataInstancesList.size()];
        int counter = 0;
        for (DynamicDataInstance inst : dataInstancesList) {
            dataInstances[counter] = inst;
            counter++;
        }

    }

    @Override
    public int getNumberOfDataInstances() {
        return 0;
    }

    @Override
    public DataInstance getDataInstance(int i) {
        return null;
    }

    @Override
    public Attributes getAttributes() {
        return null;
    }

    @Override
    public DataInstance nextDataInstance() {
        return null;
    }

    @Override
    public boolean hasMoreDataInstances() {
        return false;
    }

    @Override
    public void restart() {

    }
}
