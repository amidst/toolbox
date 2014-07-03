package eu.amidst.core.DynamicDataBase;

import eu.amidst.core.DynamicDataBase.DynamicDataHeader;

/**
 * Created by afa on 03/07/14.
 */
public class SequenceStream {
    public DynamicDataHeader getDynamicDataHeader() {
        return null;
    }

    public int getMarkovOrder() {
        return 0;
    }

    public boolean hasMoreData() {
        return false;
    }

    public SequenceData nextSequenceData() {
        return null;
    }

    public void reset() {
    }

    public boolean isReseteable() {
        return false;
    }
}
