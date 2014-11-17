package eu.amidst.core.database.filereaders.arffWekaReader;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;

import java.util.Set;

/**
 * Created by ana@cs.aau.dk on 17/11/14.
 */
public class WekaAttributes  implements Attributes {

    Set<Attribute> attributes;

    public WekaAttributes(Set<Attribute> attributes){
        this.attributes = attributes;
    }

    @Override
    public Set<Attribute> getSet() {
        return attributes;
    }

    @Override
    public void print() {

    }

    @Override
    public Attribute getAttributeByName(String name) {
        for(Attribute att: attributes){
            if(att.getName().equals(name))
                return att;
        }
        throw new UnsupportedOperationException("Attribute "+name+" is not part of the set of Attributes");
    }
}
