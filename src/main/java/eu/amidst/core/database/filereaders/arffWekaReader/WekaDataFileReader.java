package eu.amidst.core.database.filereaders.arffWekaReader;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.filereaders.DataFileReader;
import eu.amidst.core.database.filereaders.DataRow;
import eu.amidst.core.variables.MultinomialStateSpace;
import eu.amidst.core.variables.RealStateSpace;
import eu.amidst.core.variables.StateSpace;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.*;
import java.util.*;

/**
 * Created by ana@cs.aau.dk on 14/11/14.
 */
public class WekaDataFileReader implements DataFileReader, Iterator<DataRow>{

    private ArffLoader arffLoader = null;
    private Instances dataStructure;
    private Attributes attributes;
    /* We need to read an instance in advance to know if we have reached the EOF*/
    private Instance present = null;

    public WekaDataFileReader(String s){

        try {
            arffLoader = new ArffLoader();
            File file = new File(s);
            arffLoader.setFile(file);
            dataStructure = arffLoader.getStructure();
        }catch(IOException e){
            throw new UnsupportedOperationException(e);
        }

        /*Convert attributes to AMIDST format*/
        weka.core.Attribute attrWeka;
        Enumeration attributesWeka = dataStructure.enumerateAttributes();
        List<Attribute> attrs = new ArrayList<>();
        while (attributesWeka.hasMoreElements()) {
            attrWeka = (weka.core.Attribute) attributesWeka.nextElement();
            StateSpace stateSpaceAtt=null;
            if(attrWeka.isNominal()){
                String[] vals = new String[attrWeka.numValues()];
                for (int i=0; i<attrWeka.numValues(); i++) {
                    vals[i] = attrWeka.value(i);
                }
                stateSpaceAtt = new MultinomialStateSpace(attrWeka.numValues());
            }else{
                stateSpaceAtt = new RealStateSpace();
            }
            Attribute att = new Attribute(attrWeka.index(),attrWeka.name(),stateSpaceAtt);
            attrs.add(att);
        }
        attributes = new Attributes(attrs);

        /*Read one instance*/
        try {
            present = arffLoader.getNextInstance(dataStructure);
        }catch(IOException e){
            throw new UnsupportedOperationException(e);
        }

    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public DataRow next() {
        Instance inst = present;
        if(inst==null){
            return null;
        }
        try {
            present = arffLoader.getNextInstance(dataStructure);
        }catch(IOException e){
            throw new UnsupportedOperationException(e);
        }
        return new DataRowWeka(inst);
    }

    @Override
    public boolean hasNext() {
        return present!=null;
    }

    @Override
    public void restart() {
        try {
            arffLoader.reset();
        }catch(IOException e){
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public boolean doesItReadThisFileExtension(String fileExtension) {
        return fileExtension.equals(".arff");
    }

    @Override
    public Iterator<DataRow> iterator() {
        return this;
    }
}
