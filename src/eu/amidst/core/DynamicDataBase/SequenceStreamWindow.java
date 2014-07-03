package eu.amidst.core.DynamicDataBase;

/**
 * Created by afa on 03/07/14.
 */
public class SequenceStreamWindow {
    public DynamicDataHeader getDynamicDataHeader() {
        return null;
    }

    public int getMarkovOrder() {
        return 0;
    }

    public int getWindowSize() {
        return 0;
    }

    public boolean hasMoreData() {
        return false;
    }

    public void loadNextWindow() {
    }

    public SequenceData getSequenceData(int indexInWindow) {
        return null;
    }

    public boolean isReseteable() {
        return false;
    }

    public void reset() {
    }
}
