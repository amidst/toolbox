package eu.amidst.core.DynamicDataBase;

/**
 * Created by afa on 03/07/14.
 */
public class BucketSequenceData {
    public int getMarkovOrder() {
        return 0;
    }

    public boolean hasMoreData() {
        return false;
    }

    public SequenceData nextSequenceData() {
        return null;
    }

    public boolean isReseteable() {
        return false;
    }

    public void reset() {
    }
}
