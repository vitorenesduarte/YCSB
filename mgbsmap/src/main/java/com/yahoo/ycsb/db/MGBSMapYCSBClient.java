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
import org.telecomsudparis.smap.SMapClient;
import org.telecomsudparis.smap.ClientConfig;
import org.telecomsudparis.smap.pb.*;
import scala.util.Either;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * This is a client implementation for MGB-SMap 0.1-SNAPSHOT.
 */
public class MGBSMapYCSBClient extends DB {

  private static ClientConfig cfg;
  private SMapServiceClient ycsbSMapClientService;
  private static volatile boolean verbose = false;

  public MGBSMapYCSBClient() {

  }

  /**
   * Initialize any state for this DB. Called once per DB instance; there is one
   * DB instance per client thread.
   */
  public void init() throws DBException {
    synchronized (MGBSMapYCSBClient.class) {
      if(cfg == null) {
        verbose = Boolean.valueOf(getProperties().getProperty("verbose"));
        String zhost = getProperties().getProperty("host");
        String zport = getProperties().getProperty("port");
        String mgbHost = SMapServiceClient.javaClientGetClosestNode(zhost, zport);
        cfg = new ClientConfig(zhost, zport, "undefined", 8980, mgbHost);
      }
    }
    ycsbSMapClientService = new SMapServiceClient(cfg);
  }

   /**
   * Read a record from the database. Each field/value pair from the result will
   * be stored in a HashMap.
   *
   * @param table  The name of the table
   * @param key    The record key of the record to read.
   * @param fields The list of fields to read, or null for all of them
   * @param result A HashMap of field/value pairs for the result
   * @return Zero on success, a non-zero error code on error
   */
  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    HashMap<String, String> fieldsMap = new HashMap<>();
    Smap.Item readItem;

    //FIXME: Define callerId
    if(fields != null) {
      fieldsMap.keySet().addAll(fields);
      readItem = Smap.Item.newBuilder().setKey(key).putAllFields(fieldsMap).build();
    } else {
      readItem = Smap.Item.newBuilder().setKey(key).build();
    }
    Smap.MapCommand readCmd = Smap.MapCommand.newBuilder().
            setItem(readItem).
            setOperationType(Smap.MapCommand.OperationType.GET).
            setOperationUuid(SMapClient.uuid()).
            build();

    //FIXME: This is equivalent to use .getOrElse(), so use it.
    Either<Exception, ResultsCollection> eitherRes = ycsbSMapClientService.sendCmd(MapCommand.fromJavaProto(readCmd));
    if(eitherRes.isRight()){
      ResultsCollection res = eitherRes.right().get();
      Smap.ResultsCollection javaResult = ResultsCollection.toJavaProto(res);
      StringByteIterator.putAllAsByteIterators(result, javaResult.getResultsList().get(0).getFieldsMap());
      if (verbose) {
        System.out.println("READ: " + key + " -> " + result);
      }
      return Status.OK;
    } else {
      return Status.ERROR;
    }

  }

  /**
   * Perform a range scan for a set of records in the database. Each field/value
   * pair from the result will be stored in a HashMap.
   *
   * @param table       The name of the table
   * @param startkey    The record key of the first record to read.
   * @param recordcount The number of records to read
   * @param fields      The list of fields to read, or null for all of them
   * @param result      A Vector of HashMaps, where each HashMap is a set field/value
   *                    pairs for one record
   * @return Zero on success, a non-zero error code on error
   */
  @Override
  public Status scan(String table, String startkey, int recordcount, Set<String> fields,
                     Vector<HashMap<String, ByteIterator>> result) {

    HashMap<String, String> fieldsMap = new HashMap<>();
    Smap.Item scanItem;
    if(fields != null) {
      fieldsMap.keySet().addAll(fields);
      scanItem = Smap.Item.newBuilder().setKey(startkey).putAllFields(fieldsMap).build();
    } else {
      scanItem = Smap.Item.newBuilder().setKey(startkey).build();
    }

    //FIXME: Define callerId
    Smap.MapCommand scanCmd = Smap.MapCommand.newBuilder().
            setItem(scanItem).
            setRecordcount(recordcount).
            setStartKey(startkey).
            setOperationType(Smap.MapCommand.OperationType.SCAN).
            setOperationUuid(SMapClient.uuid()).
            build();

    //FIXME: This is equivalent to use .getOrElse(), so use it.
    Either<Exception, ResultsCollection> eitherRes = ycsbSMapClientService.sendCmd(MapCommand.fromJavaProto(scanCmd));
    if(eitherRes.isRight()){
      ResultsCollection res = eitherRes.right().get();
      Smap.ResultsCollection javaResult = ResultsCollection.toJavaProto(res);

      for (Smap.Item it : javaResult.getResultsList()) {
        HashMap<String, ByteIterator> tempFieldsMap = new HashMap<>();
        StringByteIterator.putAllAsByteIterators(tempFieldsMap, it.getFieldsMap());
        result.add(tempFieldsMap);
      }

      if (verbose) {
        System.out.println("SCAN: " + startkey + "[0-" + recordcount + "] -> " + result.toString());
      }
      return Status.OK;
    } else {
      return Status.ERROR;
    }

  }

  /**
   * Update a record in the database. Any field/value pairs in the specified
   * values HashMap will be written into the record with the specified record
   * key, overwriting any existing values with the same field name.
   *
   * @param table  The name of the table
   * @param key    The record key of the record to write.
   * @param values A HashMap of field/value pairs to update in the record
   * @return Zero on success, a non-zero error code on error
   */
  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    if (verbose) {
      System.out.println("READ: " + key + " -> " + values);
    }
    HashMap<String, String> fieldsMap = new HashMap<>();
    StringByteIterator.putAllAsStrings(fieldsMap, values);

    //FIXME: Define callerId
    Smap.Item updateItem = Smap.Item.newBuilder().setKey(key).putAllFields(fieldsMap).build();
    Smap.MapCommand updateCmd = Smap.MapCommand.newBuilder().
            setItem(updateItem).
            setOperationType(Smap.MapCommand.OperationType.UPDATE).
            setOperationUuid(SMapClient.uuid()).
            build();

    //FIXME: This is equivalent to use .getOrElse(), so use it.
    Either<Exception, ResultsCollection> eitherRes = ycsbSMapClientService.sendCmd(MapCommand.fromJavaProto(updateCmd));
    if(eitherRes.isRight()){
      //ResultsCollection res = eitherRes.right().get();
      return Status.OK;
    } else {
      return Status.ERROR;
    }

  }

  /**
   * Cleanup any state for this DB. Called once per DB instance; there is one DB
   * instance per client thread.
   */
  @Override
  public void cleanup() {
    //ycsbSMapClientService.shutdown();
  }

  /**
   * Insert a record in the database. Any field/value pairs in the specified
   * values HashMap will be written into the record with the specified record
   * key.
   *
   * @param table  The name of the table
   * @param key    The record key of the record to insert.
   * @param values A HashMap of field/value pairs to insert in the record
   * @return Zero on success, a non-zero error code on error
   */
  @Override
  public Status insert(String table, String key, Map<String, ByteIterator> values) {
    if (verbose) {
      System.out.println("INSERT: " + key + " -> " + values);
    }
    HashMap<String, String> fieldsMap = new HashMap<>();
    StringByteIterator.putAllAsStrings(fieldsMap, values);

    //FIXME: Define callerId
    Smap.Item insertItem = Smap.Item.newBuilder().setKey(key).putAllFields(fieldsMap).build();
    Smap.MapCommand insertCmd = Smap.MapCommand.newBuilder().
            setItem(insertItem).
            setOperationType(Smap.MapCommand.OperationType.INSERT).
            setOperationUuid(SMapClient.uuid()).
            build();

    //FIXME: This is equivalent to use .getOrElse(), so use it.
    Either<Exception, ResultsCollection> eitherRes = ycsbSMapClientService.sendCmd(MapCommand.fromJavaProto(insertCmd));
    if(eitherRes.isRight()){
      //ResultsCollection res = eitherRes.right().get();
      return Status.OK;
    } else {
      return Status.ERROR;
    }

  }

  /**
   * Delete a record from the database.
   *
   * @param table The name of the table
   * @param key   The record key of the record to delete.
   * @return Zero on success, a non-zero error code on error
   */
  @Override
  public Status delete(String table, String key) {
    if (verbose) {
      System.out.println("DELETE: " + key);
    }
    //FIXME: Define callerId
    Smap.Item deleteItem = Smap.Item.newBuilder().setKey(key).build();
    Smap.MapCommand deleteCmd = Smap.MapCommand.newBuilder().
            setItem(deleteItem).
            setOperationType(Smap.MapCommand.OperationType.DELETE).
            setOperationUuid(SMapClient.uuid()).
            build();

    //FIXME: This is equivalent to use .getOrElse(), so use it.
    Either<Exception, ResultsCollection> eitherRes = ycsbSMapClientService.sendCmd(MapCommand.fromJavaProto(deleteCmd));
    if (eitherRes.isRight()) {
      //ResultsCollection res = eitherRes.right().get();
      return Status.OK;
    } else {
      return Status.ERROR;
    }
  }

}
