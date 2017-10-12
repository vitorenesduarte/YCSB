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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


import server.VCDMapServer;
import client.VCDMapClient;

/**
 * This is a client implementation for VCD-Map 0.1-SNAPSHOT.
 */
public class VCDMapYCSBClient extends DB {

  private VCDMapServer<String, Map<String, String>> server1 = new VCDMapServer<>("table1");
  private VCDMapClient<String, Map<String, String>> client1 = new VCDMapClient<>("Client1", server1);

  public VCDMapYCSBClient() {

  }

  public void init() throws DBException {
    server1.serverInit();
  }

  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    scala.Option<Map<String, String>> r = client1.sendGet(key);
    //This should be done with getOrElse but I'm tired
    HashMap<String, String> defaultRow = new HashMap<>();
    if (r.isEmpty()) {
      StringByteIterator.putAllAsByteIterators(result, defaultRow);
    } else {
      HashMap<String, String> row = new HashMap<>(r.get());
      StringByteIterator.putAllAsByteIterators(result, row);
    }
    return Status.OK;
  }

  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
                     Vector<HashMap<String, ByteIterator>> result) {
    return Status.OK;
  }

  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    HashMap<String, String> row = new HashMap<>();
    StringByteIterator.putAllAsStrings(row, values);
    client1.sendUpdate(key, row);
    return Status.OK;
  }

  @Override
  public Status insert(String table, String key, Map<String, ByteIterator> values) {
    HashMap<String, String> row = new HashMap<>();
    StringByteIterator.putAllAsStrings(row, values);
    client1.sendPut(key, row);
    return Status.OK;
  }

  @Override
  public Status delete(String table, String key) {
    client1.sendDelete(key);
    return Status.OK;
  }

}