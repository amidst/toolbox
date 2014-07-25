package eu.amidst.core.DynamicDataBase;

/**
 * Created by afa on 03/07/14.
 */
public interface BucketSequenceStream {

    public DynamicDataHeader getDynamicDataHeader();

    public int getMarkovOrder();

    public boolean hasMoreData();

    public BucketSequenceData nextBucketSequenceData();

    public boolean isReseteable();

    public void reset();
}
