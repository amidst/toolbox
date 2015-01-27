package eu.amidst.core.database.dynamics;

import eu.amidst.core.database.filereaders.DynamicDataInstanceImpl;

/**
 * Created by afa on 03/07/14.
 */
public interface BucketSequenceData {
    int getMarkovOrder();

    boolean hasMoreData();

    DynamicDataInstanceImpl nextSequenceData();

    boolean isReseteable();

    void reset();
}