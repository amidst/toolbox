package eu.amidst.core.database.statics;

import eu.amidst.core.header.statics.StaticDataHeader;

/**
 * Created by afa on 02/07/14.
 */
public interface DataStreamWindow {
    public int getWindowSize();

    public void loadNextDataWindow();

    public DataInstance getDataInstance(int indexInWindow);

    public boolean isRestartable();

    public void restart();

    public StaticDataHeader getStaticDataHeader();
}
