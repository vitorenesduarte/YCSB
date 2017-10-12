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
import go.bindings.Bindings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * This is a client implementation for Infinispan 5.x.
 */
public class EPaxosClient extends DB {

  private Bindings.Parameters parameters = Bindings.NewParameters();

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

    if (!getProperties().containsKey("host") | !getProperties().containsKey("port")) {
      parameters.Connect("localhost", 7087, leaderless, fast);
    } else {
      parameters.Connect(
          getProperties().getProperty("host"),
          Integer.parseInt(getProperties().getProperty("host")),
          leaderless,
          fast);

    }
  }

  @Override
  public void cleanup() {
  }

  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    parameters.Read(key.hashCode());
    return Status.OK;
  }

  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
                     Vector<HashMap<String, ByteIterator>> result) {
    return Status.ERROR;
  }

  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    parameters.Write(key.hashCode(), values.toString());
    return Status.OK;
  }

  @Override
  public Status insert(String table, String key, Map<String, ByteIterator> values) {
    parameters.Write(key.hashCode(), values.toString());
    return Status.OK;
  }

  @Override
  public Status delete(String table, String key) {
    parameters.Write(key.hashCode(), "");
    return Status.OK;
  }

}
