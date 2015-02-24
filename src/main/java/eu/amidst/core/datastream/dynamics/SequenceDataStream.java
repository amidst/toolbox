package eu.amidst.core.datastream.dynamics;


import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.datastream.Attributes;

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
