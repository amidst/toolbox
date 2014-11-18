package eu.amidst.core.database;

import java.util.Collections;
import java.util.Set;

/**
 * Created by sigveh on 10/16/14.
 */
public class Attributes {

    Set<Attribute> attributes;



    public Attributes(Set<Attribute> attributes){
        this.attributes = Collections.unmodifiableSet(attributes);
    }

    public Set<Attribute> getSet(){
        return attributes;
    }

    public void print(){}

    public Attribute getAttributeByName(String name){
        for(Attribute att: getSet()){
            if(att.getName().equals(name))
                return att;
        }
        throw new UnsupportedOperationException("Attribute "+name+" is not part of the set of Attributes (try uppercase)");
    }
}
