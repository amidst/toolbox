package eu.amidst.core.database.dynamics;


import eu.amidst.core.database.Attributes;

/**
 * Created by afa on 03/07/14.
 */
public interface BucketSequenceStream {

    public Attributes getDynamicAttributes();

    public int getMarkovOrder();

    public boolean hasMoreData();

    public BucketSequenceData nextBucketSequenceData();

    public boolean isReseteable();

    public void reset();
}
