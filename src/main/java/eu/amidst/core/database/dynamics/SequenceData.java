package eu.amidst.core.database.dynamics;


import eu.amidst.core.database.statics.readers.DataInstance;

/**
 * Created by afa on 03/07/14.
 */
public interface SequenceData {

    public double getValueCurrentTime(int varID);

    public double getValuePreviousTime(int varID, int previousTime);

    public double getTimeID();

    public int getNumTimeStepsBack();

    public SequenceDataStream getSequenceDataStream();

    public DataInstance getDataInstance();

}
