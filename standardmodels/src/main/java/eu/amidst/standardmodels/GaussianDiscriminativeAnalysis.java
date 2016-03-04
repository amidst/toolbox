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
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.DAG;

/**
 * Created by andresmasegosa on 4/3/16.
 */
public class GaussianDiscriminativeAnalysis extends Model {
    @Override
    protected DAG buildDAG(Attributes attributes) {


        return null;
    }

    public static void main(String[] args) {


        DataStream<DataInstance> data = DataStreamLoader.openFromFile("tmp.arff");

        Model model = new GaussianDiscriminativeAnalysis();

        model.learnModel(data);

        System.out.println(model);


        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(1000)) {
            model.updateModel(batch);
        }


        System.out.println(model);

    }

}
