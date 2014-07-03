package eu.amidst.core.StaticDataBase;

/**
 * Created by afa on 02/07/14.
 */
public class DataStream {
    public DataInstance nextDataInstance() {
        return null;
    }

    public boolean hasMoreDataInstances() {
        return false;
    }

    public boolean isRestartable() {
        return false;
    }

    public void restart() {
    }

    public StaticDataHeader getStaticDataHeader() {
        return null;
    }
}
