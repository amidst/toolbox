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

package eu.amidst.nips2016.nipspapers;

import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_Dirichlet;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_TruncatedExponential;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 6/6/16.
 */
public class Main {

    public static void main(String[] args) {

        Variables variables = new Variables();
        Variable word = variables.newSparseMultionomialVariable("A",2);
        //word = variables.newMultionomialVariable(attributes.getAttributeByName(wordDocumentName));

        Variable topicIndicator = variables.newMultinomialVariable("TopicIndicator", 1);

        DAG dagLDA = new DAG(variables);
        dagLDA.setName("LDA");

        dagLDA.getParentSet(word).addParent(topicIndicator);


        List<EF_ConditionalDistribution> dists = dagLDA.getParentSets().stream()
                .map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents()))
                .collect(Collectors.toList());

        EF_LearningBayesianNetwork ef_learningmodel = new EF_LearningBayesianNetwork(dists);



        EF_Dirichlet dirichletOld = (EF_Dirichlet)ef_learningmodel.getDistributionList().get(2);
        EF_Dirichlet dirichletUnif = Serialization.deepCopy(dirichletOld);
        EF_Dirichlet dirichletNew = Serialization.deepCopy(dirichletOld);

        Variable expVar = variables.newTruncatedExponential("EXPO");
        EF_TruncatedExponential ef_truncatedExponential = expVar.getDistributionType().newEFUnivariateDistribution();

        double alpha = 1.0;

        for (int i = 0; i < 10; i++) {


            double nUnif = 0.1;
            dirichletUnif.getNaturalParameters().set(0, nUnif);
            dirichletUnif.getNaturalParameters().set(1, 1000);
            dirichletUnif.updateMomentFromNaturalParameters();

            double distOld = 0.01;
            double NOld = 1e6;
            dirichletOld.getNaturalParameters().set(0, NOld * distOld);
            dirichletOld.getNaturalParameters().set(1, NOld * (1 - distOld));
            dirichletOld.updateMomentFromNaturalParameters();

            double distNew = 0.05;
            double Nnew = 9.7e3;
            dirichletNew.getNaturalParameters().set(0, Nnew * distNew + alpha * NOld * distOld + (1 - alpha) * nUnif);
            dirichletNew.getNaturalParameters().set(1, Nnew * (1 - distNew) + alpha * NOld * (1 - distOld) + (1 - alpha) * nUnif);
            dirichletNew.updateMomentFromNaturalParameters();

            double klOld = dirichletNew.kl(dirichletOld.getNaturalParameters(), dirichletOld.computeLogNormalizer());

            double klUnif = dirichletNew.kl(dirichletUnif.getNaturalParameters(), dirichletUnif.computeLogNormalizer());

            System.out.println(klOld);
            System.out.println(klUnif);
            System.out.println(klUnif - klOld);

            ef_truncatedExponential.getNaturalParameters().set(0, klUnif - klOld);
            ef_truncatedExponential.updateMomentFromNaturalParameters();
            System.out.println(ef_truncatedExponential.getMomentParameters().get(0));
            alpha = ef_truncatedExponential.getMomentParameters().get(0);
            System.out.println();
        }
    }
}
