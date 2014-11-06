package eu.amidst.core.database.statics.readers;


/**
 * Created by afa on 02/07/14.
 */
public interface DataStream {
   // interface DataStream<E extends Enum> extends Iterable<DataInstance<E>> {
    public DataInstance nextDataInstance();

    public boolean hasMoreDataInstances();

    public boolean isRestartable();

    public void restart();

    public Attributes getAttributes();
}
