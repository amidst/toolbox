package eu.amidst.core.potential;

import java.util.List;

/**
 * Created by afa on 03/07/14.
 */
public interface Potential {

    void setVariables(List variables);

    List getVariables();

    void combine(Potential pot);

    void marginalize(List variables);
}
