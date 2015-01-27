package eu.amidst.core.database.dynamics;


import eu.amidst.core.database.filereaders.DynamicDataInstanceImpl;
import eu.amidst.core.database.Attributes;

/**
 * Created by afa on 03/07/14.
 */
public interface SequenceDataStream {
    Attributes getDynamicAttributes();

    int getNumTimeStepsBack();

    boolean hasMoreData();

    DynamicDataInstanceImpl nextSequenceData();

    void reset();

    boolean isReseteable();
}
