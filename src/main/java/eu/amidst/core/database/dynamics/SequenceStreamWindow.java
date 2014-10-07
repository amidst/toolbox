package eu.amidst.core.database.dynamics;


import eu.amidst.core.header.dynamics.DynamicDataHeader;

/**
 * Created by afa on 03/07/14.
 */
public interface SequenceStreamWindow {

    public DynamicDataHeader getDynamicDataHeader();

    public int getMarkovOrder();

    public int getWindowSize();

    public boolean hasMoreData();

    public void loadNextWindow();

    public SequenceData getSequenceData(int indexInWindow);

    public boolean isReseteable();

    public void reset();
}
