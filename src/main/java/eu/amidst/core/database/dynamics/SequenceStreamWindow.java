package eu.amidst.core.database.dynamics;


import eu.amidst.core.database.dynamics.readers.impl.DynamicDataInstance;
import eu.amidst.core.database.statics.readers.Attributes;

/**
 * Created by afa on 03/07/14.
 */
public interface SequenceStreamWindow {

    public Attributes getDynamicAttributes();

    public int getWindowSize();

    public boolean hasMoreData();

    public void loadNextWindow();

    public DynamicDataInstance getSequenceData(int indexInWindow);

    public boolean isReseteable();

    public void reset();
}
