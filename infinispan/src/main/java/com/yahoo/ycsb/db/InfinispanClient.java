package com.yahoo.ycsb.db;

import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.StringByteIterator;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.indexing.LocalIndexBuilder;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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

   private static AtomicInteger clientCounter = new AtomicInteger();
   private static final Log logger = LogFactory.getLog(InfinispanClient.class);
   private static BasicCacheContainer infinispanManager;
   private static BasicCache<String, Map<String, String>> cache;

   private static final String CACHE_NAME_ENV_VARIABLE = "LEADS_YCSB_CACHE_NAME";

   public InfinispanClient() {
   }

   @Override
   public void init() throws DBException {
      
      synchronized (InfinispanClient.class) {

         if (clientCounter.getAndIncrement()==0) {

            String host = getProperties().getProperty("host");
            _debug = Boolean.parseBoolean(getProperties().getProperty("debug", "false"));

            if (host == null)
               throw new RuntimeException("Required property \"host\" missing for InfinispanClient");

            try {

               Properties properties = new Properties();
               properties.setProperty("infinispan.client.hotrod.default_executor_factory.pool_size", "100");
               infinispanManager = new EnsembleCacheManager(
                     host,
                     null,
                     properties,
                     new LocalIndexBuilder());
               infinispanManager.start();

               createCache(System.getenv());
               cache.start();

               if (_debug)
                  System.out.println("Manager created");

            } catch (Exception e) {
               e.printStackTrace();
            }

         }

      }

      if (_debug) System.out.println("Client linked to "+Thread.currentThread().getName()+" created");

   }

   private void createCache(final Map<String, String> env) {
		String cacheName = env.get(CACHE_NAME_ENV_VARIABLE);
		if(cacheName == null) {
			if (_debug) System.out.println("Use the default cache");
			cache = infinispanManager.getCache();
		} else {
			cache = infinispanManager.getCache(cacheName);
			if (_debug) System.out.println("Use the cache with name " + cacheName);
		}

   }

   @Override
   public void cleanup() {
      synchronized (InfinispanClient.class) {
         if (clientCounter.decrementAndGet()==0) {
            cache.stop();
            infinispanManager.stop();
            infinispanManager = null;
         }
      }
   }

   @Override
   public int read(String table, String key, Set<String> fields, HashMap<String, ByteIterator> result) {
      try {
         Map<String, String> row;
         Object fromCache = cache.get(key);
         try {
             Map<Object, Object> m = (Map<Object, Object>) fromCache;
             row = (Map) m;
         } catch(java.lang.ClassCastException e1) {
             //e1.printStackTrace();
             row = null;
         }

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

   @Override
   public int scan(String table, String startkey, int recordcount, Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
      logger.warn("Infinispan does not support scan semantics");
      return OK;
   }

   @Override
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

   @Override
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

   @Override
   public int delete(String table, String key) {
      try {
         cache.remove(key);
         return OK;
      } catch (Exception e) {
         return ERROR;
      }
   }
}
