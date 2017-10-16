/**
 * Copyright (c) 2012-2016 YCSB contributors. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.yahoo.ycsb.db;

import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.Status;
import com.yahoo.ycsb.StringByteIterator;
import go.bindings.Bindings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * This is a client implementation for EPaxos.
 */
public class EPaxosClient extends DB {

  private Bindings.Parameters epaxos = Bindings.NewParameters();
  private boolean verbose = false;

  public EPaxosClient() {
  }

  @Override
  public void init() throws DBException {

    boolean leaderless = false;
    if ("true".equals(getProperties().getProperty("leaderless"))) {
      leaderless = true;
    }

    boolean fast = false;
    if ("true".equals(getProperties().getProperty("fast"))) {
      fast = true;
    }

    if ("true".equals(getProperties().getProperty("verbose"))) {
      verbose = true;
    }

    if (!getProperties().containsKey("host") | !getProperties().containsKey("port")) {
      epaxos.Connect("localhost", 7087, leaderless, fast);
    } else {
      epaxos.Connect(
          getProperties().getProperty("host"),
          Integer.parseInt(getProperties().getProperty("port")),
          leaderless,
          fast);

    }
  }

  @Override
  public void cleanup() {
  }

  @Override
  public Status insert(String table, String key, Map<String, ByteIterator> values) {
    try {
      byte[] data = marshal(StringByteIterator.getStringMap(values));
      epaxos.Write(hash(key), data);
      if (verbose) {
        System.out.println("UPDATE: " + key + " -> " + values);
      }
      return Status.OK;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Status.ERROR;
  }

  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    try {
      result = new HashMap<>();
      byte[] data = epaxos.Read(hash(key));
      StringByteIterator.putAllAsByteIterators(result, unmarshal(data));
      if (verbose) {
        System.out.println("READ: " + key + " -> " + result);
      }
      return Status.OK;
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return Status.ERROR;
  }

  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
                     Vector<HashMap<String, ByteIterator>> result) {
    try {
      result = new Vector<>();
      HashMap<String, ByteIterator> item = new HashMap<>();
      byte[] data = epaxos.Scan(hash(startkey));
      StringByteIterator.putAllAsByteIterators(item, unmarshal(data));
      result.add(item);
      if (verbose) {
        System.out.println("SCAN: " + startkey + "[0-" + recordcount + "] -> " + result.toString());
      }
      return Status.OK;
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return Status.ERROR;
  }

  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    return Status.NOT_IMPLEMENTED;
  }


  @Override
  public Status delete(String table, String key) {
    return Status.NOT_IMPLEMENTED;
  }

  // adapted from String.hashCode()
  public static long hash(String string) {
    long h = 1125899906842597L; // prime
    int len = string.length();

    for (int i = 0; i < len; i++) {
      h = 31 * h + string.charAt(i);
    }
    return h;
  }

  private static byte[] marshal(Map<String, String> map) throws IOException {
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(byteOut);
    out.writeObject(map);
    return byteOut.toByteArray();
  }

  private static Map<String, String> unmarshal(byte[] data) throws IOException, ClassNotFoundException {
    ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
    ObjectInputStream in = new ObjectInputStream(byteIn);
    return (Map<String, String>) in.readObject();
  }

}
