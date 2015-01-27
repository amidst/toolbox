package eu.amidst.core.database.dynamics;


import eu.amidst.core.database.DynamicDataInstance;
import eu.amidst.core.database.Attributes;

/**
 * Created by afa on 03/07/14.
 */
public interface SequenceDataStream {
    Attributes getDynamicAttributes();

    int getNumTimeStepsBack();

    boolean hasMoreData();

    DynamicDataInstance nextSequenceData();

    void reset();

    boolean isReseteable();
}
