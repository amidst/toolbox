
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
package eu.amidst.modelExperiments;

import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.learning.parametric.IdenitifableModelling;

import java.io.Serializable;

/**
 * Created by ana@cs.aau.dk <mailto:ana@cs.aau.dk> on 15/02/16.
 */
public class IdentifiableMixtureModel implements IdenitifableModelling, Serializable {

    private int numLocalHiddenVariables;
    private int nStates;

    public IdentifiableMixtureModel(int numLocalHiddenVariables, int nStates){
        this.numLocalHiddenVariables = numLocalHiddenVariables;
        this.nStates = nStates;
    }

    /**
     * Represents the serial version ID for serializing the object.
     */
    private static final long serialVersionUID =4107783324901370839L;

    @Override
    public int getNumberOfEpochs() {
        return numLocalHiddenVariables+2;
    }


    @Override
    public boolean isActiveAtEpoch(Variable variable,int epoch) {

        if (epoch==0) {
            return true;
        }


        int multinomialIndex = -1;
        for (int i = 0; i < nStates; i++) {
            if (variable.getName().contains("MultinomialHidden = " + i)){
                multinomialIndex = i;
                break;
            }
        }

        if (multinomialIndex==-1){

        }

        if (variable.getName().contains("Beta0"))
            return epoch%getNumberOfEpochs() ==0;
        else if (variable.getName().contains("Gamma"))
            return epoch%getNumberOfEpochs() ==1;
        else if (variable.getName().contains("Beta_LocalHidden")) {
            for (int i = 0; i < this.numLocalHiddenVariables; i++) {
                if (variable.getName().contains("Beta_LocalHidden_"+i))
                    return epoch % getNumberOfEpochs() == i+2;
            }
            return true;
        }else if (variable.getName().contains("_Mean_Parameter_")){
            return epoch%getNumberOfEpochs()==0;
        }
        else
            return true;
    }

}


