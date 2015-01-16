package eu.amidst.core.models;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.huginlink.ConverterToAMIDST;
import eu.amidst.core.huginlink.DBNConverterToAmidst;

/**
 * Created by Hanen on 15/01/15.
 */
public class DynamicBayesianNetworkLoader {

        public static DynamicBayesianNetwork loadDBNFromHugin(String file) throws ExceptionHugin {

            ParseListener parseListener = new DefaultClassParseListener();
            Class huginDBN = new Class(new ClassCollection());
            DynamicBayesianNetwork amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);
            return amidstDBN;
        }

}
