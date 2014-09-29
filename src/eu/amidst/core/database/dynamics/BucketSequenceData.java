package eu.amidst.core.database.dynamics;

/**
 * Created by afa on 03/07/14.
 */
public interface BucketSequenceData {
    public int getMarkovOrder();

    public boolean hasMoreData();

    public SequenceData nextSequenceData();

    public boolean isReseteable();

    public void reset();
}
