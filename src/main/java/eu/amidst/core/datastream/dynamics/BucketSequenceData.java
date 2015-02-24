package eu.amidst.core.datastream.dynamics;


import eu.amidst.core.datastream.DynamicDataInstance;

/**
 * Created by afa on 03/07/14.
 */
public interface BucketSequenceData {
    int getMarkovOrder();

    boolean hasMoreData();

    DynamicDataInstance nextSequenceData();

    boolean isReseteable();

    void reset();
}