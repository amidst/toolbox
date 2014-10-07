package eu.amidst.core.database.statics;

/**
 * Created by afa on 02/07/14.
 */
public interface DataInstance<E extends Enum> {
    public double getValue(E index);

    public void setValue(int varID, double value);

    //public DataStream getDataStream();
}

