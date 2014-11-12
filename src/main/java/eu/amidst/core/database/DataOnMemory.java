package eu.amidst.core.database;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataOnMemory extends DataOnDisk {

    public int getNumberOfDataInstances();

    public DataInstance getDataInstance(int i);

}
