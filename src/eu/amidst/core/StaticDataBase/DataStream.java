package eu.amidst.core.StaticDataBase;

import eu.amidst.core.headers.StaticDataHeader;

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
