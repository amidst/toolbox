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

package eu.amidst.core.learning.parametric.bayesian;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.exponentialfamily.EF_TruncatedExponential;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;

import java.util.Map;

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class DriftSVB extends SVB{


    EF_TruncatedExponential ef_TExpP;
    EF_TruncatedExponential ef_TExpQ;

    Variable truncatedExpVar;

    boolean firstBatch=false;

    CompoundVector posteriorT_1=null;

    CompoundVector prior=null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initLearning() {
        super.initLearning();
        truncatedExpVar = this.getDAG().getVariables().newTruncatedExponential("TruncatedExponentialVar");
        this.ef_TExpP = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(0.1);
        this.ef_TExpQ = truncatedExpVar.getDistributionType().newEFUnivariateDistribution(0.1);
        firstBatch=false;
        prior = this.plateuStructure.getPlateauNaturalParameterPrior();
    }

    public double updateModelWithConceptDrift(DataOnMemory<DataInstance> batch) {
        if (firstBatch){
            firstBatch=false;
            this.plateuStructure.setEvidence(batch.getList());
            this.plateuStructure.runInference();

            posteriorT_1 = this.plateuStructure.getPlateauNaturalParameterPosterior();
            return this.plateuStructure.getLogProbabilityOfEvidence();
        }

        boolean convergence = false;
        double elbo = Double.NaN;
        double niter=0;
        while(!convergence && niter<100) {

            //Messages for TExp to Theta
            double lambda = this.ef_TExpQ.getMomentParameters().get(0);
            CompoundVector newPrior = Serialization.deepCopy(prior);
            prior.multiplyBy(1 - lambda);
            CompoundVector newPosterior = Serialization.deepCopy(posteriorT_1);
            newPosterior.multiplyBy(lambda);
            newPrior.sum(newPosterior);
            this.plateuStructure.updateNaturalParameterPrior(newPrior);

            //Standard Messages
            this.plateuStructure.getVMP().setMaxIter(10);
            this.plateuStructure.runInference();

            //Messages to TExp
            this.plateuStructure.updateNaturalParameterPrior(this.prior);
            double kl_q_p0 = this.plateuStructure.getNonReplictedNodes().mapToDouble(node-> {
                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                return node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                        node.getPDist().getExpectedLogNormalizer(momentParents));
            }).sum();

            this.plateuStructure.updateNaturalParameterPrior(this.posteriorT_1);
            double kl_q_pt_1 = this.plateuStructure.getNonReplictedNodes().mapToDouble(node-> {
                Map<Variable, MomentParameters> momentParents = node.getMomentParents();
                return node.getQDist().kl(node.getPDist().getExpectedNaturalFromParents(momentParents),
                        node.getPDist().getExpectedLogNormalizer(momentParents));
            }).sum();

            ef_TExpQ.getNaturalParameters().set(0,
                    kl_q_pt_1 - kl_q_p0 +
                    this.ef_TExpP.getNaturalParameters().get(0));
            ef_TExpQ.fixNumericalInstability();
            ef_TExpQ.updateMomentFromNaturalParameters();


            //Compute elbo
            double newELBO = this.plateuStructure.getLogProbabilityOfEvidence();
            //Elbo component assocaited to the truncated exponential.
            newELBO-=this.ef_TExpQ.kl(this.ef_TExpP.getNaturalParameters(),this.ef_TExpP.computeLogNormalizer());

            if (!Double.isNaN(elbo) &&  newELBO<elbo){
                new IllegalStateException("Non increasing lower bound");
            }
            if (!Double.isNaN(elbo) && (newELBO-elbo)/elbo<this.plateuStructure.getVMP().getThreshold()){
                convergence=true;
            }

            niter++;
        }



        return elbo;
    }


    public double getLambdaValue(){
        return this.ef_TExpQ.getMomentParameters().get(0);
    }


    public static void main(String[] args) {

    }
}
