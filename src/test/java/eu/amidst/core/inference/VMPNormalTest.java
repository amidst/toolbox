package eu.amidst.core.inference;

//import cern.jet.random.Normal;
import com.google.common.base.Stopwatch;
import eu.amidst.core.distribution.*;
import eu.amidst.core.exponentialfamily.EF_Multinomial;
import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 10/02/15.
 */
public class VMPNormalTest extends TestCase {


    public static void test1() throws IOException, ClassNotFoundException{

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");
        for (int i = 0; i < 1; i++) {

            //bn.randomInitialization(new Random(i));
            //System.out.println(bn.toString());

            InferenceEngineForBN.setModel(bn);

            Stopwatch watch = Stopwatch.createStarted();
            InferenceEngineForBN.compileModel();
            System.out.println(watch.stop());

            bn.getStaticVariables().getListOfVariables().forEach( var -> System.out.println(var.getName()+": "+InferenceEngineForBN.getPosterior(bn.getStaticVariables().getVariableByName(var.getName())).toString()));
        }
    }
}
