package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataOnMemoryFromFile implements DataOnMemory, DataOnDisk, DataOnStream {

    DataFileReader reader;
    /**
     * We assume that the dataset here is going to be relatively small and that it is going to be read multiple times
     * so it is better to store it in an array, otherwise it might be just better to keep it in the ArrayList and avoid
     * the extra pass in the constructor.
     */
    StaticDataInstance[] dataInstances;
    int pointer = 0;


    public StaticDataOnMemoryFromFile(DataFileReader reader) {
        this.reader = reader;

        List<StaticDataInstance> dataInstancesList = new ArrayList<>();

        while (reader.hasMoreDataRows()) {
            dataInstancesList.add(new StaticDataInstance(reader.nextDataRow()));
        }
        reader.reset();

        dataInstances = new StaticDataInstance[dataInstancesList.size()];
        int counter = 0;
        for (StaticDataInstance inst : dataInstancesList) {
            dataInstances[counter] = inst;
            counter++;
        }

    }

    @Override
    public int getNumberOfDataInstances() {
        return dataInstances.length;
    }

    @Override
    public DataInstance getDataInstance(int i) {
        return dataInstances[i];
    }

    @Override
    public DataInstance nextDataInstance() {
        pointer++;
        if (pointer > getNumberOfDataInstances()) {
            throw new UnsupportedOperationException("You must call restart() to go sequentially through the dataset again");
        }
        return dataInstances[pointer];
    }

    @Override
    public boolean hasMoreDataInstances() {
        return pointer == getNumberOfDataInstances();
    }

    public void restart() {
        pointer = 0;
    }

    public Attributes getAttributes(){ return reader.getAttributes();}
}