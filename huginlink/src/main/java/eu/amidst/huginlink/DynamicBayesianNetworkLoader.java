package eu.amidst.huginlink;

import COM.hugin.HAPI.ClassCollection;
import COM.hugin.HAPI.ExceptionHugin;
import COM.hugin.HAPI.Class;
import eu.amidst.core.models.DynamicBayesianNetwork;

/**
 * Created by Hanen on 15/01/15.
 */
public class DynamicBayesianNetworkLoader {

    public static DynamicBayesianNetwork loadDBNFromHugin(String file) throws ExceptionHugin {

            Class huginDBN = new Class(new ClassCollection());
            DynamicBayesianNetwork amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);
            return amidstDBN;
        }
}
