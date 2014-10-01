package eu.amidst.core.database.dynamics;


import eu.amidst.core.header.dynamics.DynamicDataHeader;

/**
 * Created by afa on 03/07/14.
 */
public interface SequenceDataStream {
    public DynamicDataHeader getDynamicDataHeader();

    public int getNumTimeStepsBack();

    public boolean hasMoreData();

    public SequenceData nextSequenceData();

    public void reset();

    public boolean isReseteable();
}
