package eu.amidst.core.database.filereaders.arffWekaReader;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.filereaders.DataFileReader;
import eu.amidst.core.database.filereaders.DataRow;
import eu.amidst.core.header.StateSpaceType;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.*;
import java.util.*;

/**
 * Created by ana@cs.aau.dk on 14/11/14.
 */
public class WekaDataFileReader implements DataFileReader{

    ArffLoader arffLoader = null;
    Instances dataStructure;
    Attributes attributes;
    /* We need to read an instance in advance to know if we have reached the EOF*/
    Instance present = null;

    public WekaDataFileReader(String s){

        try {
            arffLoader = new ArffLoader();
            File file = new File(s);
            arffLoader.setFile(file);
            dataStructure = arffLoader.getStructure();
        }catch(IOException e){};

        /*Convert attributes to AMIDST format*/
        weka.core.Attribute attrWeka;
        Enumeration attributesWeka = dataStructure.enumerateAttributes();
        List<Attribute> attrs = new ArrayList<>();
        while (attributesWeka.hasMoreElements()) {
            attrWeka = (weka.core.Attribute) attributesWeka.nextElement();
            StateSpaceType stateSpaceTypeAtt;
            if(attrWeka.isNominal()){
                stateSpaceTypeAtt = StateSpaceType.MULTINOMIAL;
            }else{
                stateSpaceTypeAtt = StateSpaceType.REAL;
            }
            Attribute att = new Attribute(attrWeka.index(),attrWeka.name(),stateSpaceTypeAtt, attrWeka.numValues());
            attrs.add(att);
        }
        attributes = new Attributes(attrs);

        /*Read one instance*/
        try {
            present = arffLoader.getNextInstance(dataStructure);
        }catch(IOException e){};

    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public DataRow nextDataRow() {
        Instance inst = present;
        if(inst==null)
            return null;
        try {
            present = arffLoader.getNextInstance(dataStructure);
        }catch(IOException e){};
        return new DataRowWeka(inst);
    }

    @Override
    public boolean hasMoreDataRows() {
        return present!=null;
    }

    @Override
    public void reset() {
        try {
            arffLoader.reset();
        }catch(IOException e){};
    }

    @Override
    public boolean doesItReadThisFileExtension(String fileExtension) {
        return fileExtension.equals(".arff");
    }

}
