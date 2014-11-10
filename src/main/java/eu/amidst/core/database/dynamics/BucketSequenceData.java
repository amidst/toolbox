package eu.amidst.core.database.dynamics;

import eu.amidst.core.database.dynamics.readers.impl.DynamicDataInstance;

/**
 * Created by afa on 03/07/14.
 */
public interface BucketSequenceData {
    public int getMarkovOrder();

    public boolean hasMoreData();

    public DynamicDataInstance nextSequenceData();

    public boolean isReseteable();

    public void reset();
}
