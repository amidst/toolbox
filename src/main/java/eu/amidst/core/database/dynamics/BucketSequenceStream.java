package eu.amidst.core.database.dynamics;


import eu.amidst.core.database.Attributes;

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
