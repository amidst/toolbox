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
package eu.amidst.core.utils;


import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Utility class for object serialization/deserialization and deep copies.
 */
public class Serialization {

    /**
     * Serializes a given object
     * @param object, any serializable object.
     * @return An array of bytes
     *
     */
    public static byte[] serializeObject(Object object){

        try{
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = null;
            try {
                out = new ObjectOutputStream(bos);
                out.writeObject(object);
                out.close();
                byte[] result = bos.toByteArray();
                bos.close();
                return result;
            } catch (Exception ex) {
                throw new UndeclaredThrowableException(ex);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException ex) {
                    // ignore close exception
                    return null;
                }
                try {
                    bos.close();
                } catch (IOException ex) {
                    // ignore close exception
                    return null;
                }
            }
        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }
    }

    /**
     * Deserilizes an object from an array of bytes.
     * @param bytes, an array of bytes
     * @param <T> The type of the deserialized object.
     * @return A properly deseralized object
     */
    public static <T> T deserializeObject(byte[] bytes) {

        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            T result = (T) in.readObject();
            bis.close();
            in.close();
            return result ;
        } catch (ClassNotFoundException ex) {
            throw new UndeclaredThrowableException(ex);
        } catch (IOException ex) {
            throw new UndeclaredThrowableException(ex);
        } finally {
            try {
                bis.close();
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }


    /**
     * Performs a deep copy of a given object by serialization.
     * @param object, the object to be copied.
     * @param <T>, the type of the object to be copied
     * @return A valid deep copied object.
     */
    public static <T> T deepCopy(T object){
        return deserializeObject(serializeObject(object));
    }

}