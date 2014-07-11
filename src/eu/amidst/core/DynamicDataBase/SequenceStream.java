package eu.amidst.core.DynamicDataBase;

import eu.amidst.core.DynamicDataBase.DynamicDataHeader;

/**
 * Created by afa on 03/07/14.
 */
public interface SequenceStream {
    public DynamicDataHeader getDynamicDataHeader();

    public int getMarkovOrder();

    public boolean hasMoreData();

    public SequenceData nextSequenceData();

    public void reset();

    public boolean isReseteable();
}
