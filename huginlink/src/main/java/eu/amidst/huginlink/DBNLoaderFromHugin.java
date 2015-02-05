package eu.amidst.huginlink;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;

/**
 * Created by afa on 5/2/15.
 */
public class DBNLoaderFromHugin {

        /**
         * Load the class "className" (and all classes instantiated in
         * that class, recursively).  Then "unfold" the class to a Domain
         * that can be used for inference.
         */
        public DBNLoaderFromHugin(String className)
        {
            try {
                ClassCollection cc = new ClassCollection();
                DefaultClassParseListener parseListener = new DefaultClassParseListener();
                cc.parseClasses (className + ".oobn", parseListener);
                Class main = cc.getClassByName (className);
                if (main == null)
                    System.out.println ("Class not found: " + className);
                else {
                    Domain domain = main.createDomain();
                    // Use the domain for inference, etc. (omitted)
                    domain.delete();
                }
                cc.delete();
            } catch (ExceptionHugin e) {
                System.out.println ("Exception caught: " + e.getMessage());
            } catch (Exception e) {
                System.out.println ("General exception: " + e.getMessage());
                e.printStackTrace();
            }
        }

     public static void main(String[] args) throws Exception {

         DBNLoaderFromHugin dbn = new DBNLoaderFromHugin("huginDBNFromAMIDST");


     }

}
