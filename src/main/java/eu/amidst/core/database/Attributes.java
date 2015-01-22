package eu.amidst.core.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by sigveh on 10/16/14.
 */
public class Attributes implements Serializable {

    private static final long serialVersionUID = -1877629684033612201L;

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
            if(!name.equals("TIME_ID") && !name.equals("SEQUENCE_ID")){
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
}
