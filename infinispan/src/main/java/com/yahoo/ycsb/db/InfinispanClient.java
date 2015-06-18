package com.yahoo.ycsb.db;

import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.StringByteIterator;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.*;

/**
 * This is a client implementation for Infinispan 6.x.
 *
 * Some settings:
 *
 * @author Manik Surtani (manik AT jboss DOT org)
 * @author Pierre Sutra
 *
 */
public class InfinispanClient extends DB {

    private static final int OK = 0;
    private static final int ERROR = -1;
    private static final int NOT_FOUND = -2;

    private boolean _debug = false;

    private BasicCacheContainer infinispanManager;
    private BasicCache<String, Map<String, String>> cache;

    private static final Log logger = LogFactory.getLog(InfinispanClient.class);

    public InfinispanClient() {
    }

    public void init() throws DBException {

        String host = getProperties().getProperty("host");
        String zhost = getProperties().getProperty("zhost");
        System.out.println("HERE"+getProperties().toString());
        String replicationFactor = getProperties().getProperty("replicationFactor");
        _debug = Boolean.parseBoolean(getProperties().getProperty("debug", "false"));

        if (host == null)
            throw new RuntimeException("Required property \"host\" missing for InfinispanClient");

        try {
            if (zhost == null) {
                infinispanManager = (BasicCacheContainer) new RemoteCacheManager(host+":11222");
            }else{
                infinispanManager = new EnsembleCacheManager(Arrays.asList(host.split(",")));
            }
            cache = infinispanManager.getCache();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void cleanup() {
        infinispanManager.stop();
        infinispanManager = null;
    }

    public int read(String table, String key, Set<String> fields, HashMap<String, ByteIterator> result) {
        try {
            Map<String, String> row;
            row = cache.get(key);
            if (row != null) {
                result.clear();
                if (fields == null || fields.isEmpty()) {
                    StringByteIterator.putAllAsByteIterators(result, row);
                } else {
                    for (String field : fields){
                        result.put(field, new StringByteIterator(row.get(field)));
                    }
                }
            }
            if (_debug)
                System.out.println("Reading key: " + key+"("+result+")");
            return OK;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
    }

    public int scan(String table, String startkey, int recordcount, Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
        logger.warn("Infinispan does not support scan semantics");
        return OK;
    }

    public int update(String table, String key, HashMap<String, ByteIterator> values) {
        try {
            Map<String, String> row = cache.get(key);
            if (row == null) {
                row = StringByteIterator.getStringMap(values);
                cache.put(key, row);
            } else {
                StringByteIterator.putAllAsStrings(row, values);
            }

            return OK;
        } catch (Exception e) {
            return ERROR;
        }
    }

    public int insert(String table, String key, HashMap<String, ByteIterator> values) {
        try {
            Map<String, String> row = new HashMap<String, String>();
            row = StringByteIterator.getStringMap(values);
            if (_debug)
                System.out.println("Inserting key: " + key+"("+row+")");
            cache.put(key,row);
            return OK;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
    }

    public int delete(String table, String key) {
        try {
            cache.remove(key);
            return OK;
        } catch (Exception e) {
            return ERROR;
        }
    }
}
