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

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.io.BNLoaderFromHugin;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Created by afa on 10/3/15.
 */
public class HuginInferenceForBNTest {


    @Test
    public void test() throws IOException, ClassNotFoundException, ExceptionHugin {

        Domain huginBN = BNLoaderFromHugin.loadFromFile("../networks/simulated/Example.net");
        BayesianNetwork bn = BNConverterToAMIDST.convertToAmidst(huginBN);
        System.out.println(bn.toString());

        Variables variables = bn.getDAG().getVariables();

        Variable varA = variables.getVariableByName("A");
        Variable varB = variables.getVariableByName("B");
        Variable varC = variables.getVariableByName("C");
        Variable varD = variables.getVariableByName("D");
        Variable varE = variables.getVariableByName("E");


        //**************************************************************************************************************
        // HUGIN
        //**************************************************************************************************************
        HuginInference inferenceHuginForBN = new HuginInference();
        inferenceHuginForBN.setModel(bn);

        Random random = new Random(0);
        for (int i = 0; i < 10; i++) {


            HashMapAssignment assignment = new HashMapAssignment(4);
            assignment.setValue(varA, random.nextDouble());
            assignment.setValue(varB, random.nextDouble());
            assignment.setValue(varC, random.nextDouble());
            //assignment.setValue(varD, 7.0);
            assignment.setValue(varE, random.nextDouble());

            //**************************************************************************************************************
            // HUGIN
            //**************************************************************************************************************


            inferenceHuginForBN.setEvidence(assignment);
            inferenceHuginForBN.runInference();

            //Multinomial postHuginA = ((Multinomial)inferenceHuginForBN.getPosterior(varA));
            //Multinomial postHuginB = ((Multinomial)inferenceHuginForBN.getPosterior(varB));
            //Normal postHuginC = ((Normal)inferenceHuginForBN.getPosterior(varC));
            Normal postHuginD = ((Normal) inferenceHuginForBN.getPosterior(varD));
            //Normal postHuginE = ((Normal)inferenceHuginForBN.getPosterior(varE));


            //**************************************************************************************************************
            // AMIDST - VMP
            //**************************************************************************************************************

            VMP vmp = new VMP();
            vmp.setModel(bn);
            vmp.setEvidence(assignment);
            vmp.runInference();

            //Multinomial postVMP_A = ((Multinomial)vmp.getPosterior(varA));
            //Multinomial postVMP_B = ((Multinomial)vmp.getPosterior(varB));
            //Normal postVMP_C = ((Normal)vmp.getPosterior(varC));
            Normal postVMP_D = ((Normal) vmp.getPosterior(varD));
            //Normal postVMP_E = ((Normal)vmp.getPosterior(varE));

            //**************************************************************************************************************
            // TESTS
            //**************************************************************************************************************

            //assertTrue(postHuginA.equalDist(postVMP_A, 0.01));
            //assertTrue(postHuginB.equalDist(postVMP_B,0.01));
            //assertTrue(postHuginC.equalDist(postVMP_C,0.01));
            assertTrue(postHuginD.equalDist(postVMP_D, 0.0001));
            //assertTrue(postHuginE.equalDist(postVMP_E,0.01));
        }
    }
}