package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class StaticDataOnMemoryFromFile implements DataOnMemory, DataOnDisk, DataOnStream {

    private DataFileReader reader;

    /**
     * We assume that the dataset here is going to be relatively small and that it is going to be read multiple times
     * so it is better to store it in an array, otherwise it might be just better to keep it in the ArrayList and avoid
     * the extra pass in the constructor.
     */
    private StaticDataInstance[] dataInstances;


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
    public Attributes getAttributes() {
        return this.reader.getAttributes();
    }

    @Override
    public Stream<DataInstance> stream() {
        return Arrays.stream(this.dataInstances);
    }

    @Override
    public void close() {
        this.reader.close();
    }

}