package eu.amidst.core.DynamicDataBase;

import eu.amidst.core.StaticDataBase.DataInstance;

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
