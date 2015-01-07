package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public interface SufficientStatistics extends Vector {
    public static SufficientStatistics sum(SufficientStatistics vec1, SufficientStatistics vec2){
        return vec1;//(SufficientStatistics) Vector.sum(vec1,vec2);
    }
}
