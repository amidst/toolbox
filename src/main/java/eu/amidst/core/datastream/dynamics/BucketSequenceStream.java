package eu.amidst.core.datastream.dynamics;


import eu.amidst.core.datastream.Attributes;

/**
 * Created by afa on 03/07/14.
 */
public interface BucketSequenceStream {

    Attributes getDynamicAttributes();

    int getMarkovOrder();

    boolean hasMoreData();

    BucketSequenceData nextBucketSequenceData();

    boolean isReseteable();

    void reset();
}
