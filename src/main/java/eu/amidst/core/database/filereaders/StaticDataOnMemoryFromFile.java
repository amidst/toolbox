package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataOnMemoryFromFile implements DataOnMemory, DataOnDisk, DataOnStream, Iterator<DataInstance> {

    private DataFileReader reader;
    /**
     * We assume that the dataset here is going to be relatively small and that it is going to be read multiple times
     * so it is better to store it in an array, otherwise it might be just better to keep it in the ArrayList and avoid
     * the extra pass in the constructor.
     */
    private StaticDataInstance[] dataInstances;
    private int pointer = 0;


    public StaticDataOnMemoryFromFile(DataFileReader reader) {
        this.reader = reader;

        List<StaticDataInstance> dataInstancesList = new ArrayList<>();

        for (DataRow row: reader){
            dataInstancesList.add(new StaticDataInstance(row));
        }
        reader.restart();

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
    public DataInstance next() {
        if (pointer >= getNumberOfDataInstances()) {
            throw new UnsupportedOperationException("Make sure to call hasNext() to know when the sequence " +
                    "has finished (restart() moves the reader pointer to the beginning");
        }
        return dataInstances[pointer++];
    }

    @Override
    public boolean hasNext() {
        return pointer < getNumberOfDataInstances();
    }

    public void restart() {
        pointer = 0;
    }

    public Attributes getAttributes(){ return reader.getAttributes();}

    @Override
    public Iterator<DataInstance> iterator() {
        return this;
    }
}