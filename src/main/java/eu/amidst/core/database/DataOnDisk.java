/**
 * ************ ISSUE LIST ***************
 *
 * 1. (Andres) Add a "close" method to close the possible linked file or whatever.
 *
 * 2. (Andres) Implements as Iterable();
 *
 */



package eu.amidst.core.database;


import java.util.Iterator;

/**
 * Created by afa on 02/07/14.
 */
public interface DataOnDisk<E extends DataInstance> extends DataBase<E>{

    void restart();

}
