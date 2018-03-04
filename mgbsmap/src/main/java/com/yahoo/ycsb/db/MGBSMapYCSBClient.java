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


import org.telecomsudparis.smap.MapCommand;
import org.telecomsudparis.smap.ResultsCollection;
import org.telecomsudparis.smap.SMapServiceClient;
import org.telecomsudparis.smap.pb.*;


//import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * This is a client implementation for MGB-SMap 0.1-SNAPSHOT.
 */
public class MGBSMapYCSBClient extends DB {

  private static volatile boolean localReads = false;
  private static volatile boolean verbose = false;
  private SMapServiceClient ycsbSMapClientService;

  public MGBSMapYCSBClient() {

  }

  public void init() throws DBException {
    String host = getProperties().getProperty("host");
    ycsbSMapClientService = new SMapServiceClient(host, 8980);
  }

  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    if (verbose) {
      System.out.println("READ: " + key + " -> " + result);
    }
    HashMap<String, String> defaultRow = new HashMap<>();
    defaultRow.keySet().addAll(fields);
    //FIXME: Define callerId
    Smap.Item readIt = Smap.Item.newBuilder().setKey(key).putAllFields(defaultRow).build();
    Smap.MapCommand readCmd = Smap.MapCommand.newBuilder().
            setItem(readIt).
            setOperationType(Smap.MapCommand.OperationType.GET).
            setOperationUuid(java.util.UUID.randomUUID().toString()).
            build();
    //isRight?
    ResultsCollection res = ycsbSMapClientService.sendCmd(MapCommand.fromJavaProto(readCmd)).right().get();
    Smap.ResultsCollection javaResult = ResultsCollection.toJavaProto(res);

    StringByteIterator.putAllAsByteIterators(result,
    javaResult.getResultsList().get(0).getFieldsMap());

    return Status.OK;
  }

  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
                     Vector<HashMap<String, ByteIterator>> result) {
    if (verbose) {
      System.out.println("SCAN: " + startkey + "[0-" + recordcount + "] -> " + result.toString());
    }
    return Status.OK;
  }

  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    if (verbose) {
      System.out.println("UPDATE: " + key + " -> " + values);
    }
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
    return Status.OK;
  }

  @Override
  public Status delete(String table, String key) {
    return Status.OK;
  }

}
