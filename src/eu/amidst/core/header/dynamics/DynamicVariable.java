package eu.amidst.core.header.dynamics;

import eu.amidst.core.header.statics.Variable;

/**
 * Created by afa on 02/07/14.
 */
public interface DynamicVariable extends Variable {

    public int getTimeVarID(int previousTime);

    public boolean isTemporalConnected();

    public void setTemporalConnected(boolean isTemporalConnected);

}
