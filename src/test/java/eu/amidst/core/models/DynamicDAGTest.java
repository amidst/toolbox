package eu.amidst.core.models;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
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


    DataOnDisk data = new DynamicDataOnDiskFromFile(new ARFFDataReader("datasets/syntheticDataDaimler.arff"));

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

        Variable vlatSIGMA = dynamicVariables.addObservedDynamicVariable(attVLATSIGMA);
        Variable vlatMEAS = dynamicVariables.addObservedDynamicVariable(attVLATMEAS);
        Variable olatSIGMA = dynamicVariables.addObservedDynamicVariable(attOLATSIGMA);
        Variable olatMEAS = dynamicVariables.addObservedDynamicVariable(attOLATMEAS);

        Variable vlatREAL = dynamicVariables.addRealDynamicVariable(vlatMEAS);
        Variable olatREAL = dynamicVariables.addRealDynamicVariable(olatMEAS);

        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName("A_LAT");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpace(new RealStateSpace());
        variableBuilder.setDistributionType(DistType.GAUSSIAN);
        Variable aLAT = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("LE");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpace(new FiniteStateSpace(Arrays.asList("Yes", "No")));
        variableBuilder.setDistributionType(DistType.MULTINOMIAL_LOGISTIC);
        Variable latEv = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(vlatMEAS).addParent(vlatSIGMA);
        dynamicDAG.getParentSetTimeT(vlatMEAS).addParent(vlatREAL);
        dynamicDAG.getParentSetTimeT(olatMEAS).addParent(olatSIGMA);
        dynamicDAG.getParentSetTimeT(olatMEAS).addParent(olatREAL);
        dynamicDAG.getParentSetTimeT(aLAT).addParent(dynamicVariables.getTemporalClone(aLAT));
        dynamicDAG.getParentSetTimeT(vlatREAL).addParent(aLAT);
        dynamicDAG.getParentSetTimeT(vlatREAL).addParent(dynamicVariables.getTemporalClone(vlatREAL));
        dynamicDAG.getParentSetTimeT(olatREAL).addParent(dynamicVariables.getTemporalClone(olatREAL));
        dynamicDAG.getParentSetTimeT(olatREAL).addParent(dynamicVariables.getTemporalClone(vlatREAL));
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
