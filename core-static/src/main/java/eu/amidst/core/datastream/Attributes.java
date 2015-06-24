/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * This class acts as a container of for the {@link Attribute} objects of a data set.
 *
 */
public class Attributes implements Serializable, Iterable<Attribute> {

    /** The serial vesion ID for serializing the object */
    private static final long serialVersionUID = -1877629684033612201L;

    /** The name of attribute acting as a TIME_ID*/
    public static final String TIME_ID_ATT_NAME = "TIME_ID";

    /** The name of attribute acting as a SEQUENCE_ID*/
    public static final String SEQUENCE_ID_ATT_NAME = "SEQUENCE_ID";

    /** A list containing the  Attribue objects*/
    private List<Attribute> attributes;

    /**
     * A constructor using a List of Attribute objects
     * @param attributes, a non-empty list of Attribute objects
     */
    public Attributes(List<Attribute> attributes){
        this.attributes = Collections.unmodifiableList(attributes);
    }

    /**
     * Return the list of Attribute objects kept in this object
     * @return
     */
    public List<Attribute> getList(){
        return attributes;
    }

    /**
     * Return the number of Attribute objects
     * @return
     */
    public int getNumberOfAttributes(){
        return this.attributes.size();
    }

    /**
     * Return a list with the Attribute objects except those which
     * act as TIME_ID and SEQUENCE_ID
     * @return
     */
    //TODO This method is not standard?!?
    public List<Attribute> getListExceptTimeAndSeq(){
        List<Attribute> attributeList = new ArrayList<>();
        for(Attribute att: getList()){
            String name = att.getName();
            if(!name.equals(Attributes.TIME_ID_ATT_NAME) && !name.equals(Attributes.SEQUENCE_ID_ATT_NAME)){
                attributeList.add(att);
            }
        }
        return attributeList;
    }

    /**
     * Return the attribute with the given name
     * @param name, a valid name of an attribute
     * @return The requested Attribute object. If there is any attribute
     * with the requested name, it throws an IllegalArgumentException.
     */
    public Attribute getAttributeByName(String name){
        for(Attribute att: getList()){
            if(att.getName().equals(name)){ return att;}
        }
        throw new IllegalArgumentException("Attribute "+name+" is not part of the list of Attributes");
    }

    /**
     * Return a iterator over the attributes stored in the object.
     * @return
     */
    @Override
    public Iterator<Attribute> iterator() {
        return attributes.iterator();
    }
}
