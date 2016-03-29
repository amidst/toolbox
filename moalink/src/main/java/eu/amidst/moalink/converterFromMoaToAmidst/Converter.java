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

package eu.amidst.moalink.converterFromMoaToAmidst;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import moa.core.InstancesHeader;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * This class converts attributes from MOA to AMIDST format.
 */
public final class Converter {

    /**
     * Creates a set of {@link Attributes} from a given {@link moa.core.InstancesHeader} object.
     * @param modelContext a {@link moa.core.InstancesHeader} object.
     * @return a set of {@link Attributes}.
     */
    public static Attributes convertAttributes(InstancesHeader modelContext){
        Enumeration attributesWeka = modelContext.enumerateAttributes();
        return convertAttributes(attributesWeka, modelContext.classAttribute());
    }

    /**
     * Creates a set of {@link Attributes}, including the class, from a given {@code Enumeration}
     * of {@link weka.core.Attribute}s and a {@link weka.core.Attribute}.
     * @param attributesEnumeration an {@code Enumeration} of {@link weka.core.Attribute}s
     * @param classAtt a {@link weka.core.Attribute} object that represents the class variable.
     * @return a set of {@link Attributes}.
     */
    public static Attributes convertAttributes(Enumeration<weka.core.Attribute> attributesEnumeration,
                                               weka.core.Attribute classAtt){
        weka.core.Attribute attrWeka;
        List<Attribute> attrList = new ArrayList<>();
        /* Predictive attributes */
        while (attributesEnumeration.hasMoreElements()) {
            attrWeka = (weka.core.Attribute) attributesEnumeration.nextElement();
            convertAttribute(attrWeka,attrList);
        }
        convertAttribute(classAtt, attrList);
        return new Attributes(attrList);
    }

    /**
     * Creates a set of {@link Attributes} from a given {@code Enumeration} of {@link weka.core.Attribute}s.
     * @param attributesEnumeration an {@code Enumeration} of {@link weka.core.Attribute}s
     * @return a set of {@link Attributes}.
     */
    public static Attributes convertAttributes(Enumeration<weka.core.Attribute> attributesEnumeration){
        weka.core.Attribute attrWeka;
        List<Attribute> attrList = new ArrayList<>();
        /* Predictive attributes */
        while (attributesEnumeration.hasMoreElements()) {
            attrWeka = (weka.core.Attribute) attributesEnumeration.nextElement();
            convertAttribute(attrWeka,attrList);
        }
        return new Attributes(attrList);
    }

    /**
     * Returns the class variable.
     * @param modelContext a {@link moa.core.InstancesHeader} object.
     * @param atts a set of of {@link Attributes}.
     * @return a {@link Variable} object that represents the class variable.
     */
    public static Variable getClassVariable(InstancesHeader modelContext, Attributes atts){
        Variables variables = new Variables(atts);
        String className = modelContext.classAttribute().name();
        return variables.getVariableByName(className);
    }

    /**
     * Converts a {@link weka.core.Attribute} object to an amidst {@link Attribute} and add it to the current list of attributes.
     * @param attrWeka a {@link weka.core.Attribute} object.
     * @param attrList a {@code List} of {@link Attributes}.
     */
    private static void convertAttribute(weka.core.Attribute attrWeka, List<Attribute> attrList){
        StateSpaceType stateSpaceTypeAtt;
        if(attrWeka.isNominal()){
            String[] vals = new String[attrWeka.numValues()];
            for (int i=0; i<attrWeka.numValues(); i++) {
                vals[i] = attrWeka.value(i);
            }
            stateSpaceTypeAtt = new FiniteStateSpace(attrWeka.numValues());
        }else{
            stateSpaceTypeAtt = new RealStateSpace();
        }
        Attribute att = new Attribute(attrWeka.index(),attrWeka.name(), stateSpaceTypeAtt);
        attrList.add(att);
    }
}
