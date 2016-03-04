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

package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.learning.parametric.dVMP;

/**
 * Created by andresmasegosa on 4/3/16.
 */
public abstract class Model {

    SVB svb;

    dVMP dvmp = new dVMP();

    DAG dag;


    public void learnModel(DataOnMemory<DataInstance> datBatch){
        dvmp=null;
        dag = this.buildDAG(datBatch.getAttributes());
        svb = new SVB();
        svb.setDAG(dag);
        svb.initLearning();
        svb.updateModel(datBatch);
    }

    public void learnModel(DataStream<DataInstance> dataStream){
        dvmp=null;
        dag = this.buildDAG(dataStream.getAttributes());
        svb = new SVB();
        svb.setDAG(dag);
        svb.setDataStream(dataStream);
        svb.initLearning();
        svb.runLearning();
    }

    public void learnModel(DataFlink<DataInstance> dataFlink){

    }


    public void updateModel(DataFlink<DataInstance> dataFlink){

    }

    public void updateModel(DataOnMemory<DataInstance> datBatch){
        if (svb==null) {
            dag = this.buildDAG(datBatch.getAttributes());
            svb = new SVB();
            svb.setDAG(dag);
            svb.initLearning();
            dvmp=null;
        }

        svb.updateModel(datBatch);
    }


    public BayesianNetwork getModel(){
        if (svb!=null){
            return this.svb.getLearntBayesianNetwork();
        }

        if (this.dvmp!=null)
            return this.dvmp.getLearntBayesianNetwork();

        return null;
    }



    protected abstract DAG buildDAG(Attributes attributes);


    @Override
    public String toString() {
        return this.getModel().toString();
    }


}
