package eu.amidst.core.headers;

/**
 * Created by afa on 02/07/14.
 */
public interface DynamicVariable extends Variable {

    public int getTimeVarID(int previousTime);

    public boolean isTemporalConnected();

    public void setTemporalConnected(boolean isTemporalConnected);

}
