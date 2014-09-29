package eu.amidst.core.database.statics;

import eu.amidst.core.header.statics.StaticDataHeader;

/**
 * Created by afa on 02/07/14.
 */
public interface DataStream {
    public DataInstance nextDataInstance();

    public boolean hasMoreDataInstances();

    public boolean isRestartable();

    public void restart();

    public StaticDataHeader getStaticDataHeader();
}
