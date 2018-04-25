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

package eu.amidst.latentvariablemodels.staticmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.flinklink.core.learning.parametric.dVMP;
import eu.amidst.flinklink.core.utils.ConversionToBatches;
import eu.amidst.lda.core.BatchSpliteratorByID;
import eu.amidst.lda.core.PlateauLDA;
import eu.amidst.lda.flink.PlateauLDAFlink;

/**
 * The Model abstract class is defined as a superclass to all static standard models (not used for classification, if so,
 * extends Classifier)
 *
 * Created by andresmasegosa on 4/3/16.
 */
public class LDA extends Model<LDA> {

    int ntopics=5;
    private PlateauLDA plateauLDA;
    private PlateauLDAFlink plateauLDAFlink;

    public LDA(Attributes attributes) {
        super(attributes);
    }

    public int getNtopics() {
        return ntopics;
    }

    public LDA setNtopics(int ntopics) {
        this.ntopics = ntopics;
        return this;
    }

    @Override
    protected void initLearningFlink() {
        if(learningAlgorithmFlink==null) {
            dVMP dvmp = new dVMP();
            dvmp.setBatchSize(100);
            dvmp.setMaximumGlobalIterations(10);
            dvmp.setMaximumLocalIterations(100);
            dvmp.setLocalThreshold(1);
            dvmp.setGlobalThreshold(1);

            plateauLDAFlink = new PlateauLDAFlink(this.atts,"word","count");
            plateauLDAFlink.setNTopics(ntopics);
            dvmp.setPlateuStructure(plateauLDAFlink);
            dvmp.setBatchConverter(ConversionToBatches::toBatchesBySeqID);
            learningAlgorithmFlink = dvmp;
        }

        learningAlgorithmFlink.setBatchSize(100);

        learningAlgorithmFlink.initLearning();
        initialized=true;
    }

    @Override
    protected  void initLearning() {
        if(learningAlgorithm==null) {
            SVB svb = new SVB();
            plateauLDA = new PlateauLDA(this.atts, "word", "count");
            plateauLDA.setNTopics(ntopics);
            svb.setPlateuStructure(plateauLDA);
            svb.getPlateuStructure().getVMP().setTestELBO(false);
            svb.getPlateuStructure().getVMP().setMaxIter(100);
            svb.getPlateuStructure().getVMP().setThreshold(0.01);
            learningAlgorithm = svb;
        }
        learningAlgorithm.setWindowsSize(100);
        learningAlgorithm.setOutput(true);
        learningAlgorithm.initLearning();
        initialized=true;
    }

    @Override
    public double updateModel(DataStream<DataInstance> dataStream){
        if (!initialized)
            initLearning();


        return BatchSpliteratorByID.
                streamOverDocuments(dataStream, this.windowSize).sequential().mapToDouble(batch -> {
                    System.out.println("Batch: "+ batch.getNumberOfDataInstances());
                    return this.learningAlgorithm.updateModel(batch);
                }).sum();

    }

    public BayesianNetwork getModel(){
        if (learningAlgorithm !=null){
            this.learningAlgorithm.setDAG(plateauLDA.getDagLDA());
            return this.learningAlgorithm.getLearntBayesianNetwork();
        }

        if (learningAlgorithmFlink!=null) {
            this.learningAlgorithmFlink.setDAG(plateauLDAFlink.getDagLDA());
            return this.learningAlgorithmFlink.getLearntBayesianNetwork();
        }

        return null;
    }

}
