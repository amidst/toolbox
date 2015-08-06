/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream.filereaders.arffFileReader;

import com.google.common.collect.ImmutableList;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.StateSpaceTypeEnum;

import java.util.List;

/**
 * Created by sigveh on 10/16/14.
 */
public class DrillingAttributes extends Attributes {

    private static final Attribute MFI = new Attribute(0, "MFI", "m3/s", StateSpaceTypeEnum.REAL, 0);
    private static final Attribute SPP = new Attribute(1, "MFI", "Pa", StateSpaceTypeEnum.REAL, 0);
    private static final Attribute RPM = new Attribute(2, "RPM", "1/s", StateSpaceTypeEnum.REAL, 0);


    private static List<Attribute> attributesDrilling;
    {
        attributesDrilling = ImmutableList.of(MFI, SPP, RPM);
    }

    public DrillingAttributes(){
        super(attributesDrilling);
    }

    public Attribute getMFI() {
        return MFI;
    }

    public Attribute getRPM() {
        return RPM;
    }

    public Attribute getSPP() {
        return SPP;
    }

    @Override
    public List<Attribute> getList() {
        return attributesDrilling;
    }

    @Override
    public Attribute getAttributeByName(String name) {
        return null;
    }


}
