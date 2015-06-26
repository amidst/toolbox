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
 * Created by ana@cs.aau.dk on 18/06/15.
 */
public final class Converter {

    public static Attributes convertAttributes(InstancesHeader modelContext){
        Enumeration attributesWeka = modelContext.enumerateAttributes();
        return convertAttributes(attributesWeka, modelContext.classAttribute());

    }

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


    public static Variable getClassVariable(InstancesHeader modelContext, Attributes atts){
        Variables variables = new Variables(atts);
        String className = modelContext.classAttribute().name();
        return variables.getVariableByName(className);
    }

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
