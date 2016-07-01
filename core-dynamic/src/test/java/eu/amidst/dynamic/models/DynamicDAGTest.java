/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.dynamic.models;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Hanen on 27/01/15.
 */
public class DynamicDAGTest {


    DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile("../datasets/simulated/syntheticDataDaimler.arff");

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
        Assert.assertFalse(dynamicDAG.containCycles());

        /*test the parent set*/
        Assert.assertEquals(0, dynamicDAG.getParentSetTime0(aLAT).getNumberOfParents());
        Assert.assertEquals(2, dynamicDAG.getParentSetTimeT(latEv).getNumberOfParents());

        /* test if dynamicDAG and dynamicDAG2 (containing no arcs) are equals*/
        DynamicDAG dynamicDAG2 = new DynamicDAG(dynamicVariables);
        Assert.assertFalse(dynamicDAG.equals(dynamicDAG2));

        /* define dag2 as a copy of dag and test again */

        dynamicDAG2 = dynamicDAG;

        Assert.assertTrue(dynamicDAG.equals(dynamicDAG2));

        }

    @Test
    public void testingDynamicDAGToDAG() {
        Attribute attVLATSIGMA = data.getAttributes().getAttributeByName("V_LAT_SIGMA");
        Attribute attVLATMEAS = data.getAttributes().getAttributeByName("V_LAT_MEAS");
        Attribute attOLATSIGMA = data.getAttributes().getAttributeByName("O_LAT_SIGMA");
        Attribute attOLATMEAS = data.getAttributes().getAttributeByName("O_LAT_MEAS");

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

        Variable latEv = dynamicVariables.newMultinomialLogisticDynamicVariable("LE", Arrays.asList("Yes", "No"));

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

        System.out.println(dynamicDAG.toString());


        System.out.println(dynamicDAG.toDAGTime0().toString());
        System.out.println(dynamicDAG.toDAGTimeT().toString());

    }


    }
