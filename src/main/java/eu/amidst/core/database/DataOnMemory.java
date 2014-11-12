package eu.amidst.core.database;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataOnMemory{

    public int getNumberOfDataInstances();

    public DataInstance getDataInstance(int i);

    public Attributes getAttributes();

}
