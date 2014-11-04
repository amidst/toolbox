package eu.amidst.core.header.statics;

import eu.amidst.core.database.statics.readers.Kind;

/**
 * Created by afa on 02/07/14.
 */
public interface Variable {

    public String getName();

    public int getVarID();

    public boolean isObservable();

    public int getNumberOfStates();

    public boolean isLeave();

    public void setLeave(boolean isLeave);

    public boolean isContinuous();

}
