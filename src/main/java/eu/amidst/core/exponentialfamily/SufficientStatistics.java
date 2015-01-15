package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public interface SufficientStatistics extends Vector {
    // TODO: This is not stateless operation!!!!. However it works if we accumulate in the second argument!!! TEST!!!
    public static SufficientStatistics sumSS(SufficientStatistics vec1, SufficientStatistics vec2){
        vec2.sum(vec1);
        return vec2;
    }
}
