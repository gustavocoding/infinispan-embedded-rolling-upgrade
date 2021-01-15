package org.infinispan.sample.source;

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

public class MainApp {

   public static final String CACHE_NAME = "cache";

   public static void main(String[] args) {
      System.setProperty("infinispan.deserialization.whitelist.regexps", ".*");

      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.clustering().cacheMode(CacheMode.DIST_SYNC);
      builder.compatibility().enable();

      GlobalConfigurationBuilder gcb = GlobalConfigurationBuilder.defaultClusteredBuilder();
      GlobalConfiguration globalConfiguration = gcb.build();
      DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfiguration, new ConfigurationBuilder().build(), true);

      cacheManager.defineConfiguration(CACHE_NAME, builder.build());

      HotRodServerConfiguration hotRodServerConfiguration = new HotRodServerConfigurationBuilder()
            .host("0.0.0.0")
            .port(Util.getPortOffset() + 11222)
            .build();
      HotRodServer server = new HotRodServer();
      server.start(hotRodServerConfiguration, cacheManager);
      System.out.println("Server HotRod started on port " + server.getPort());

      Cache<Integer, CustomObject> cache = cacheManager.getCache(CACHE_NAME);
      if (cache.isEmpty()) {
         for (int i = 0; i < 100; i++) {
            cache.put(i, new CustomObject("text", 1));
         }
      }
      System.out.println("Cache size = " + cache.size());
   }
}
