package eu.amidst.core.header;

import eu.amidst.core.database.statics.readers.Attribute;

/**
 * Created by afa on 02/07/14.
 */
public interface Variable {

    public String getName();

    public int getVarID();

    public boolean isObservable();

    public int getNumberOfStates();

    public StateSpaceType getStateSpaceType();

    public DistType getDistributionType();

    public boolean isTemporalClone();

    public Attribute getAttribute();
}
