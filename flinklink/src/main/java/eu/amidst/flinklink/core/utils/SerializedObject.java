/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */
package eu.amidst.flinklink.core.utils;


import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Created by andresmasegosa on 3/9/15.
 */
public class SerializedObject implements Serializable{

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = -3436599636425587512L;

    byte[] bytes;

    public SerializedObject(Object object){
        this.bytes=serializeObject(object);
    }

    public <T> T getDeserializedObject(){
        return deserializeObject(this.bytes);
    }

    public static byte[] serializeObject(Object object) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            return bos.toByteArray();
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
    }

    public static <T> T deserializeObject(byte[] bytes) {

        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            return (T) in.readObject();
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

}
