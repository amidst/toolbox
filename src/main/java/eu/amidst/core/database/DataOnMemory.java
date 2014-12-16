package eu.amidst.core.database;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataOnMemory extends DataBase{
    int getNumberOfDataInstances();
    DataInstance getDataInstance(int i);
}
