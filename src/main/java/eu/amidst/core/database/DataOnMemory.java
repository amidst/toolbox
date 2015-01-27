package eu.amidst.core.database;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataOnMemory<E extends DataInstance> extends DataBase<E>{
    int getNumberOfDataInstances();
    E getDataInstance(int i);
}
