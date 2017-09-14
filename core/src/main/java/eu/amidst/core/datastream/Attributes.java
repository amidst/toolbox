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

package eu.amidst.core.datastream;

import org.apache.commons.lang3.text.WordUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class acts as a container of the {@link Attribute} objects of a data set.
 * <p> See {@code eu.amidst.core.examples.datastream.DataStreamExample} for an example of use. </p>
 */

public class Attributes implements Serializable, Iterable<Attribute> {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = -1877629684033612201L;

    /** Represents the name of the Attribute acting as a TIME_ID. */
    public static final String TIME_ID_ATT_NAME = "TIME_ID";

    /** Represents the name of the Attribute acting as a SEQUENCE_ID. */
    public static final String SEQUENCE_ID_ATT_NAME = "SEQUENCE_ID";

    /** Represents a list containing the Attribute objects. */
    private final List<Attribute> attributes;

    /** Represents the attribute corresponding to sequence_id*/
    private Attribute seq_id;

    /** Represents the attribute corresponding to time_id*/
    private Attribute time_id;

    /**
     * Creates a new Attributes from a given List of attribute objects.
     * @param attributes a non-empty list of Attribute objects.
     */
    public Attributes(List<Attribute> attributes){
        attributes.sort((a,b) -> a.getIndex() - b.getIndex());
        this.attributes = attributes;//Collections.unmodifiableList(attributes);
        this.time_id = null;
        this.seq_id = null;
        for(Attribute att: getFullListOfAttributes()){
            String name = att.getName();
            if(name.equals(Attributes.TIME_ID_ATT_NAME)){
                this.time_id = att;
                this.time_id.setSpecialAttribute(true);
                this.time_id.setTimeId(true);
                //this.time_id.setNumberFormat(new DecimalFormat("#"));
            }else if (name.equals(Attributes.SEQUENCE_ID_ATT_NAME)){
                this.seq_id = att;
                this.seq_id.setSpecialAttribute(true);
                this.seq_id.setSeqId(true);
                //this.seq_id.setNumberFormat(new DecimalFormat("#"));
            }
        }
    }

    /** Returns a subset of Attributes**/
    public Attributes subList(int init, int end){
        return new Attributes(this.getFullListOfAttributes().subList(init,end));
    }


    /** Returns a subset of Attributes**/
    public Attributes subSet(int... a){
        List<Attribute> list = new ArrayList<>();
        for (int i : a) {
            list.add(this.getFullListOfAttributes().get(i));
        }
        return new Attributes(list);
    }
    /**
     * Returns the attribute sequence_id.
     * @return an Attribute object or null if attribute sequence_id is not present.
     */
    public Attribute getSeq_id() {
        return seq_id;
    }

    /**
     * Returns the attribute time_id.
     * @return an Attribute object or null if attribute time_id is not present.
     */
    public Attribute getTime_id() {
        return time_id;
    }

    /**
     * Returns the full list of all the Attributes, including the special ones (i.e. seq_id, time_id, etc...).
     * @return the list of this Attributes.
     */
    public List<Attribute> getFullListOfAttributes(){
        return attributes;
    }

    /**
     * Returns the number of this Attributes.
     * @return the number of this Attributes.
     */
    public int getNumberOfAttributes(){
        return this.attributes.size();
    }

    /**
     * Returns the list of this Attributes, except the TIME_ID and SEQUENCE_ID ones.
     * @return the list of this Attributes, except the TIME_ID and SEQUENCE_ID ones.
     */
    //TODO This method is not standard?!?
    public List<Attribute> getListOfNonSpecialAttributes(){
        List<Attribute> attributeList = new ArrayList<>();
        for(Attribute att: getFullListOfAttributes()){
            if (!att.isSpecialAttribute()){
                attributeList.add(att);
            }
        }
        return attributeList;
    }

    /**
     * Returns the Attribute corresponding to the given name.
     * @param name a valid name of an Attribute.
     * @return the requested Attribute object. If there is no Attribute
     * with the requested name, it throws an IllegalArgumentException.
     */
    public Attribute getAttributeByName(String name){
        for(Attribute att: getFullListOfAttributes()){
            if(att.getName().equals(name)){ return att;}
        }
        throw new IllegalArgumentException("Attribute "+name+" is not part of the list of Attributes");
    }

    /**
     * Returns an iterator over this Attributes.
     * @return an iterator over this Attributes.
     */
    @Override
    public Iterator<Attribute> iterator() {
        return attributes.iterator();
    }


    @Override
    public String toString(){

        final int FIXED_WIDTH = 80;

        String s = "";
        Iterator<Attribute> it = this.iterator();
        if (it.hasNext()) {
            s += it.next().getName();
        }
        while (it.hasNext()) {
            s += ", " + it.next().getName();
        }
        return(WordUtils.wrap(s+"\n",FIXED_WIDTH));
    }
}