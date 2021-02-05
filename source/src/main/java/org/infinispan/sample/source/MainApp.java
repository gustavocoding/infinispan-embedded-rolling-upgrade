package org.infinispan.sample.source;

import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.sample.CustomObject;
import org.infinispan.sample.Util;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;

public class MainApp {

   public static final String CACHE_NAME_1 = "cache1";
   public static final String CACHE_NAME_2 = "cache2";

   public static void main(String[] args) throws Exception {
      System.setProperty("infinispan.deserialization.whitelist.regexps", ".*");

      ConfigurationBuilder builder_tx = new ConfigurationBuilder();
      builder_tx.clustering().cacheMode(CacheMode.DIST_SYNC);
      builder_tx.transaction().transactionMode(TransactionMode.TRANSACTIONAL)
            .useSynchronization(true)
            .transactionManagerLookup(new GenericTransactionManagerLookup())
            .autoCommit(false)
            .lockingMode(LockingMode.PESSIMISTIC);
      builder_tx.expiration().lifespan(1, TimeUnit.DAYS);

      builder_tx.compatibility().enable();

      ConfigurationBuilder builder_no_tx = new ConfigurationBuilder();
      builder_no_tx.clustering().cacheMode(CacheMode.DIST_SYNC);
      builder_no_tx.expiration().lifespan(1, TimeUnit.DAYS);
      builder_no_tx.compatibility().enable();

      GlobalConfigurationBuilder gcb = GlobalConfigurationBuilder.defaultClusteredBuilder();
      GlobalConfiguration globalConfiguration = gcb.build();
      DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfiguration, new ConfigurationBuilder().build(), true);

      cacheManager.defineConfiguration(CACHE_NAME_1, builder_tx.build());
      cacheManager.defineConfiguration(CACHE_NAME_2, builder_no_tx.build());

      HotRodServerConfiguration hotRodServerConfiguration1 = new HotRodServerConfigurationBuilder()
            .name("server1")
            .host("0.0.0.0")
            .port(Util.getPortOffset() + 11222)
            .build();
      HotRodServerConfiguration hotRodServerConfiguration2 = new HotRodServerConfigurationBuilder()
            .name("server2")
            .host("0.0.0.0")
            .port(Util.getPortOffset() + 11222 + 1000)
            .build();

      HotRodServer server1 = new HotRodServer();
      server1.start(hotRodServerConfiguration1, cacheManager);
      System.out.println("Server HotRod-1 started on port " + server1.getPort());

      HotRodServer server2 = new HotRodServer();
      server2.start(hotRodServerConfiguration2, cacheManager);
      System.out.println("Server HotRod-2 started on port " + server2.getPort());

      Cache<Integer, CustomObject> cache1 = cacheManager.getCache(CACHE_NAME_1);
      cache1.getAdvancedCache().getTransactionManager().begin();
      if (cache1.isEmpty()) {
         for (int i = 0; i < 100; i++) {
            cache1.put(i, new CustomObject("text", 1));
         }
      }
      cache1.getAdvancedCache().getTransactionManager().commit();
      System.out.println("Cache-1 size = " + cache1.size());

      Cache<Integer, CustomObject> cache2 = cacheManager.getCache(CACHE_NAME_2);
      if (cache2.isEmpty()) {
         for (int i = 200; i < 500; i++) {
            cache2.put(i, new CustomObject("text", 1));
         }
      }
      System.out.println("Cache-2 size = " + cache2.size());
   }
}
