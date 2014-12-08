package eu.amidst.core.database.dynamics;


import eu.amidst.core.database.filereaders.DynamicDataInstance;
import eu.amidst.core.database.Attributes;

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
