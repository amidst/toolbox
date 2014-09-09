package eu.amidst.core.StaticBayesianNetwork;

/**
 * Created by afa on 02/07/14.
 */
public interface ParentSet {
    public void addParent(int varID);
    public int getNumberOfParents();
}
