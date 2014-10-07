package eu.amidst.core.potential;

import java.util.List;

/**
 * Created by afa on 03/07/14.
 */
public interface Potential {

    public void setVariables(List variables);

    public List getVariables();

    public void combine(Potential pot);

    public void marginalize(List variables);
}
