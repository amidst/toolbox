package eu.amidst.core.datastream.dynamics;


import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.datastream.Attributes;

/**
 * Created by afa on 03/07/14.
 */
public interface SequenceStreamWindow {

    Attributes getDynamicAttributes();

    int getWindowSize();

    boolean hasMoreData();

    void loadNextWindow();

    DynamicDataInstance getSequenceData(int indexInWindow);

    boolean isReseteable();

    void reset();
}
