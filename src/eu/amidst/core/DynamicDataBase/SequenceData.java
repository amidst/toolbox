package eu.amidst.core.DynamicDataBase;

/**
 * Created by afa on 03/07/14.
 */
public interface SequenceData {
    public double getValue(int var, int time);

    public int varTimeID();

    public int getMarkovOrder();
}
