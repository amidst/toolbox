package eu.amidst.core.database.dynamics;


import eu.amidst.core.database.filereaders.DynamicDataInstance;
import eu.amidst.core.database.Attributes;

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
