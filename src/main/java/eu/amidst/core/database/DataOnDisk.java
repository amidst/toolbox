/**
 * ************ ISSUE LIST ***************
 *
 * 1. (Andres) Add a "close" method to close the possible linked file or whatever.
 *
 */



package eu.amidst.core.database;


/**
 * Created by afa on 02/07/14.
 */
public interface DataOnDisk{

    public DataInstance nextDataInstance();

    public boolean hasMoreDataInstances();

    public Attributes getAttributes();

    public void restart();

}
