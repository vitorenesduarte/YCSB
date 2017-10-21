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

import client.VCDMapClient;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.Status;
import com.yahoo.ycsb.StringByteIterator;
import scala.Option;
import scala.util.Either;
import server.VCDMapServer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * This is a client implementation for VCD-Map 0.1-SNAPSHOT.
 */
public class VCDMapYCSBClient extends DB {

  private static VCDMapServer<Map<String, String>> server1;
  private static VCDMapClient<Map<String, String>> client1;
  private static volatile boolean localReads = false;
  private static volatile boolean verbose = false;

  public VCDMapYCSBClient() {

  }

  public void init() throws DBException {
    synchronized (VCDMapYCSBClient.class){
      if(server1 == null) {
        String host = getProperties().getProperty("host");
        String port = getProperties().getProperty("port");
        localReads = Boolean.valueOf(getProperties().getProperty("lread"));
        verbose = Boolean.valueOf(getProperties().getProperty("verbose"));

        String[] config = {"zk=" + host+":"+port};

        server1 = new VCDMapServer<>("table1", localReads, verbose, config);
        client1 = new VCDMapClient<>("Client1", true, server1);

        server1.serverInit();
      }
    }

  }

  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    if (verbose) {
      System.out.println("READ: " + key + " -> " + result);
    }
    Either<Option<Map<String, String>>, Collection<Map<String, String>>> r = client1.sendGet(key);
    HashMap<String, String> defaultRow = new HashMap<>();
    if (r.left().get().isEmpty()) {
      StringByteIterator.putAllAsByteIterators(result, defaultRow);
    } else {
      HashMap<String, String> row = new HashMap<>(r.left().get().get());
      StringByteIterator.putAllAsByteIterators(result, row);
    }
    return Status.OK;
  }

  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
                     Vector<HashMap<String, ByteIterator>> result) {
    if (verbose) {
      System.out.println("SCAN: " + startkey + "[0-" + recordcount + "] -> " + result.toString());
    }
    Either<Option<Map<String, String>>, Collection<Map<String, String>>> r = client1.sendScan(startkey, recordcount);
    Vector<Map<String, String>> values = new Vector<>(r.right().get());

    for(int i = 0; i < values.size(); i++) {
      HashMap<String, ByteIterator> item = new HashMap<>();
      StringByteIterator.putAllAsByteIterators(item, values.elementAt(i));
      result.add(item);
    }
    return Status.OK;
  }

  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    if (verbose) {
      System.out.println("UPDATE: " + key + " -> " + values);
    }
    HashMap<String, String> row = new HashMap<>();
    StringByteIterator.putAllAsStrings(row, values);
    client1.sendUpdate(key, row);
    return Status.OK;
  }

  /*
  @Override
  public void cleanup() {
    server1.serverClose();
  }
  */

  @Override
  public Status insert(String table, String key, Map<String, ByteIterator> values) {
    if (verbose) {
      System.out.println("INSERT: " + key + " -> " + values);
    }
    HashMap<String, String> row = new HashMap<>();
    StringByteIterator.putAllAsStrings(row, values);
    client1.sendInsert(key, row);
    return Status.OK;
  }

  @Override
  public Status delete(String table, String key) {
    client1.sendDelete(key);
    return Status.OK;
  }

}
