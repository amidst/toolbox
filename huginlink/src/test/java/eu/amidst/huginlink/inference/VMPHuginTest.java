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

package eu.amidst.huginlink.inference;

import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.Variable;
import junit.framework.TestCase;

import java.io.IOException;

/**
 * Created by Hanen on 19/02/15.
 */
public class VMPHuginTest extends TestCase {

    public static void testMultinomialBN() throws IOException, ClassNotFoundException{

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("../networks/dataWeka/asia.bn");

        System.out.println(bn.toString());

        Variables variables = bn.getDAG().getVariables();

        Variable varX = variables.getVariableByName("X");
        Variable varB = variables.getVariableByName("B");
        Variable varD = variables.getVariableByName("D");
        Variable varA = variables.getVariableByName("A");
        Variable varS = variables.getVariableByName("S");
        Variable varL = variables.getVariableByName("L");
        Variable varT = variables.getVariableByName("T");
        Variable varE = variables.getVariableByName("E");

        VMP vmp = new VMP();
        vmp.setModel(bn);

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varX, 0.0);
        assignment.setValue(varB, 0.0);
        assignment.setValue(varD, 0.0);
        assignment.setValue(varA, 0.0);
        assignment.setValue(varS, 0.0);
        assignment.setValue(varT, 0.0);
        assignment.setValue(varE, 0.0);

        vmp.setEvidence(assignment);

        vmp.runInference();

        Multinomial postL = ((Multinomial)vmp.getPosterior(varL));
        System.out.println("Prob of having lung cancer P(L) = " + postL.toString());

        //test with Hugin inference
        HuginInference inferenceHuginForBN = new HuginInference();
        inferenceHuginForBN.setModel(bn);
        inferenceHuginForBN.setEvidence(assignment);
        inferenceHuginForBN.runInference();

        Multinomial postHuginL = ((Multinomial)inferenceHuginForBN.getPosterior(varL));
        System.out.println("Prob of having lung cancer P(L) = " + postHuginL.toString());

        assertTrue(postL.equalDist(postHuginL, 0.01));
    }


    public static void testMultinomialNormalBN() throws IOException, ClassNotFoundException{

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("../networks/simulated/WasteIncinerator.bn");

        System.out.println(bn.toString());

        Variables variables = bn.getDAG().getVariables();

        Variable varB = variables.getVariableByName("B");
        Variable varF = variables.getVariableByName("F");
        Variable varW = variables.getVariableByName("W");
        Variable varE = variables.getVariableByName("E");
        Variable varD = variables.getVariableByName("D");
        Variable varC = variables.getVariableByName("C");
        Variable varL = variables.getVariableByName("L");
        Variable varMin = variables.getVariableByName("Min");
        Variable varMout = variables.getVariableByName("Mout");

        VMP vmp = new VMP();
        vmp.setModel(bn);

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varB, 1.0);
        assignment.setValue(varF, 0.0);
        assignment.setValue(varW, 1.0);
        assignment.setValue(varE, -3.0);
        assignment.setValue(varD, 1.5);
        assignment.setValue(varC, -1.0);
        assignment.setValue(varL, 1.0);
        assignment.setValue(varMin, -0.5);

        vmp.setEvidence(assignment);

        vmp.runInference();

        Normal postMout = ((Normal)vmp.getPosterior(varMout));
        System.out.println("Prob of metals emission P(Mout) = " + postMout.toString());

        //test with Hugin inference
        HuginInference inferenceHuginForBN = new HuginInference();
        inferenceHuginForBN.setModel(bn);
        inferenceHuginForBN.setEvidence(assignment);
        inferenceHuginForBN.runInference();

        Normal postHuginMout = ((Normal)inferenceHuginForBN.getPosterior(varMout));
        System.out.println("Prob of metals emission P(Mout) = " + postHuginMout.toString());

        assertTrue(postMout.equalDist(postHuginMout, 0.01));
    }

}
