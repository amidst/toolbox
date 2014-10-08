package eu.amidst.core.database.statics;

/**
 * Created by afa on 02/07/14.
 */
public interface DataInstance {

    public boolean hasMoreDataInstances();

    public DataInstance nextDataInstance();

    public double getValue(int varID);

    public DataStream getDataStream();
}

