package eu.amidst.core.DynamicDataBase;

import eu.amidst.core.headers.DynamicDataHeader;

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
