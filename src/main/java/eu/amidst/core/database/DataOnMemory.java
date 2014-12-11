package eu.amidst.core.database;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataOnMemory{

    int getNumberOfDataInstances();

    DataInstance getDataInstance(int i);

    Attributes getAttributes();

}
