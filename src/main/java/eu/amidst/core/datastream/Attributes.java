package eu.amidst.core.datastream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by sigveh on 10/16/14.
 */
public class Attributes implements Serializable, Iterable<Attribute> {

    private static final long serialVersionUID = -1877629684033612201L;

    public static final String TIME_ID_ATT_NAME = "TIME_ID";

    public static final String SEQUENCE_ID_ATT_NAME = "SEQUENCE_ID";

    private List<Attribute> attributes;


    public Attributes(List<Attribute> attributes){
        this.attributes = Collections.unmodifiableList(attributes);
    }

    public List<Attribute> getList(){
        return attributes;
    }

    public int getNumberOfAttributes(){
        return this.attributes.size();
    }

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

    public void print(){}

    public Attribute getAttributeByName(String name){
        for(Attribute att: getList()){
            if(att.getName().equals(name)){ return att;}
        }
        throw new UnsupportedOperationException("Attribute "+name+" is not part of the list of Attributes");
    }

    @Override
    public Iterator<Attribute> iterator() {
        return attributes.iterator();
    }
}
