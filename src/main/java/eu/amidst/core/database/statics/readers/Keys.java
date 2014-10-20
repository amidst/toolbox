package eu.amidst.core.database.statics.readers;

/**
 * Created by sigveh on 10/20/14.
 */
public class Keys {
    private int[] doubleKeys;
    private int[] intKeys;

    public Keys(int[] doubleKeys, int[] intKeys){
        this.doubleKeys = doubleKeys;
        this.intKeys = intKeys;
    }

    public int[] getDoubleKeys() {
        return doubleKeys;
    }

    public int[] getIntKeys() {
        return intKeys;
    }

}
