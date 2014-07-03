package eu.amidst.core.StaticDataBase;

/**
 * Created by afa on 02/07/14.
 */
public class DataStreamWindow {
    public int getWindowSize() {
        return 0;
    }

    public void loadNextDataWindow() {
    }

    public DataInstance getDataInstance(int indexInWindow) {
        return null;
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
