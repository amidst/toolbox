package eu.amidst.core.models;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.io.DynamicDataStreamLoader;
import eu.amidst.core.variables.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Hanen on 27/01/15.
 */
public class DynamicDAGTest {


    DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile("datasets/syntheticDataDaimler.arff");

    @Test
    public void testingDynamicDAG() {
        Attribute attVLATSIGMA = data.getAttributes().getAttributeByName("V_LAT_SIGMA");
        Attribute attVLATMEAS  = data.getAttributes().getAttributeByName("V_LAT_MEAS");
        Attribute attOLATSIGMA = data.getAttributes().getAttributeByName("O_LAT_SIGMA");
        Attribute attOLATMEAS  = data.getAttributes().getAttributeByName("O_LAT_MEAS");

        List<Attribute> attributeList = new ArrayList();
        attributeList.add(attVLATSIGMA);
        attributeList.add(attVLATMEAS);
        attributeList.add(attOLATSIGMA);
        attributeList.add(attOLATMEAS);

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable vlatSIGMA = dynamicVariables.newDynamicVariable(attVLATSIGMA);
        Variable vlatMEAS = dynamicVariables.newDynamicVariable(attVLATMEAS);
        Variable olatSIGMA = dynamicVariables.newDynamicVariable(attOLATSIGMA);
        Variable olatMEAS = dynamicVariables.newDynamicVariable(attOLATMEAS);

        Variable vlatREAL = dynamicVariables.newRealDynamicVariable(vlatMEAS);
        Variable olatREAL = dynamicVariables.newRealDynamicVariable(olatMEAS);

        Variable aLAT = dynamicVariables.newGaussianDynamicVariable("A_LAT");

        Variable latEv = dynamicVariables.newMultinomialLogisticDynamicVariable("LE",Arrays.asList("Yes", "No"));

        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(vlatMEAS).addParent(vlatSIGMA);
        dynamicDAG.getParentSetTimeT(vlatMEAS).addParent(vlatREAL);
        dynamicDAG.getParentSetTimeT(olatMEAS).addParent(olatSIGMA);
        dynamicDAG.getParentSetTimeT(olatMEAS).addParent(olatREAL);
        dynamicDAG.getParentSetTimeT(aLAT).addParent(dynamicVariables.getInterfaceVariable(aLAT));
        dynamicDAG.getParentSetTimeT(vlatREAL).addParent(aLAT);
        dynamicDAG.getParentSetTimeT(vlatREAL).addParent(dynamicVariables.getInterfaceVariable(vlatREAL));
        dynamicDAG.getParentSetTimeT(olatREAL).addParent(dynamicVariables.getInterfaceVariable(olatREAL));
        dynamicDAG.getParentSetTimeT(olatREAL).addParent(dynamicVariables.getInterfaceVariable(vlatREAL));
        dynamicDAG.getParentSetTimeT(latEv).addParent(vlatREAL);
        dynamicDAG.getParentSetTimeT(latEv).addParent(olatREAL);

        System.out.println("OOBN fragment for the LE hypothesis with acceleration (as in Figure 4.14 of D2.1)");
        System.out.println(dynamicDAG.toString());

        /* test cyclic dynamic dag */
        assertTrue(dynamicDAG.containCycles());

        /*test the parent set*/
        assertEquals(0, dynamicDAG.getParentSetTime0(aLAT).getNumberOfParents());
        assertEquals(2, dynamicDAG.getParentSetTimeT(latEv).getNumberOfParents());

        /* test if dynamicDAG and dynamicDAG2 (containing no arcs) are equals*/
        DynamicDAG dynamicDAG2 = new DynamicDAG(dynamicVariables);
        assertFalse(dynamicDAG.equals(dynamicDAG2));

        /* define dag2 as a copy of dag and test again */

        dynamicDAG2 = dynamicDAG;

        assertTrue(dynamicDAG.equals(dynamicDAG2));

        }
}
