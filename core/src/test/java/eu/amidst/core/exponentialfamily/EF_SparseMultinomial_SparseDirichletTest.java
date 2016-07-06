/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Created by andresmasegosa on 2/5/16.
 */
public class EF_SparseMultinomial_SparseDirichletTest extends TestCase {


    public static void test() {

        Variables variables = new Variables();
        ParameterVariables parameterVariables = new ParameterVariables(1);

        Variable multiA = variables.newSparseMultionomialVariable("A", 10);
        Variable dirichlet = parameterVariables.newSparseDirichletParameter("A", 10);

        EF_SparseMultinomial_SparseDirichlet dist = new EF_SparseMultinomial_SparseDirichlet(multiA,dirichlet);


        EF_SparseDirichlet distDirichlet = new EF_SparseDirichlet(dirichlet);


        EF_LearningBayesianNetwork ef_learningBayesianNetwork = new EF_LearningBayesianNetwork(Arrays.asList(dist,distDirichlet),Arrays.asList(multiA,dirichlet));



    }
}