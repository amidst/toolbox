/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.potential;

import java.util.List;

/**
 * Created by afa on 03/07/14.
 */
public class PotentialTable implements Potential {

    public PotentialTable(int nstates){

    }
    public void setValues(double[] values) {
    }

    public double[] getValues() {
        return null;
    }

    @Override
    public void setVariables(List variables) {

    }

    @Override
    public List getVariables() {
        return null;
    }

    @Override
    public void combine(Potential pot) {

    }

    @Override
    public void marginalize(List variables) {

    }

    public void normalize(){

    }

}
