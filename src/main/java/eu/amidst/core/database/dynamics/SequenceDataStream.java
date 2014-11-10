package eu.amidst.core.database.dynamics;


import eu.amidst.core.database.dynamics.readers.impl.DynamicDataInstance;
import eu.amidst.core.database.statics.readers.Attributes;

/**
 * Created by afa on 03/07/14.
 */
public interface SequenceDataStream {
    public Attributes getDynamicAttributes();

    public int getNumTimeStepsBack();

    public boolean hasMoreData();

    public DynamicDataInstance nextSequenceData();

    public void reset();

    public boolean isReseteable();
}
