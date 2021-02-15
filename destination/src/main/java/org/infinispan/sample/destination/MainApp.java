package org.infinispan.sample.destination;

import static org.infinispan.client.hotrod.ProtocolVersion.PROTOCOL_VERSION_26;

import org.infinispan.Cache;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.jboss.marshalling.commons.GenericJBossMarshaller;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.infinispan.sample.CustomObject;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.infinispan.upgrade.RollingUpgradeManager;

public class MainApp {

   public static final String CACHE_NAME_1 = "cache1";
   public static final String CACHE_NAME_2 = "cache2";

   public static void main(String[] args) throws Exception {
      System.setProperty("infinispan.deserialization.whitelist.regexps", ".*");
      String cache = System.getProperty("cache");
      if(cache == null) {
         System.out.println("Please provide cache to be migrated, cache1 or cache2, via system property, e.g. -Dcache=cache1");
         System.exit(0);
      }

      ConfigurationBuilder builder_tx = new ConfigurationBuilder();
      builder_tx.clustering().cacheMode(CacheMode.DIST_SYNC);
      builder_tx.encoding().key().mediaType(MediaType.APPLICATION_OBJECT_TYPE);
      builder_tx.encoding().value().mediaType(MediaType.APPLICATION_OBJECT_TYPE);
      builder_tx.transaction().transactionMode(TransactionMode.TRANSACTIONAL)
            .transactionManagerLookup(new EmbeddedTransactionManagerLookup())
            .useSynchronization(true)
            .autoCommit(false)
            .lockingMode(LockingMode.PESSIMISTIC);
      RemoteStoreConfigurationBuilder store1 = builder_tx.persistence().addStore(RemoteStoreConfigurationBuilder.class);
      store1.hotRodWrapping(false).rawValues(false)
            .marshaller(GenericJBossMarshaller.class)
            .protocolVersion(PROTOCOL_VERSION_26)
            .remoteCacheName(CACHE_NAME_1).shared(true)
            .addServer().host("localhost").port(11222);

      ConfigurationBuilder builder_no_tx = new ConfigurationBuilder();
      builder_no_tx.clustering().cacheMode(CacheMode.DIST_SYNC);
      builder_no_tx.encoding().key().mediaType(MediaType.APPLICATION_OBJECT_TYPE);
      builder_no_tx.encoding().value().mediaType(MediaType.APPLICATION_OBJECT_TYPE);

      RemoteStoreConfigurationBuilder store2 = builder_no_tx.persistence().addStore(RemoteStoreConfigurationBuilder.class);
      store2.hotRodWrapping(false).rawValues(false)
            .marshaller(GenericJBossMarshaller.class)
            .protocolVersion(PROTOCOL_VERSION_26)
            .remoteCacheName(CACHE_NAME_2).shared(true)
            .addServer().host("localhost").port(12222);

      GlobalConfigurationBuilder gcb = GlobalConfigurationBuilder.defaultClusteredBuilder();
      gcb.serialization().marshaller(new GenericJBossMarshaller());
      GlobalConfiguration globalConfiguration = gcb.defaultCacheName("default").build();

      DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfiguration, new ConfigurationBuilder().build(), true);

      cacheManager.defineConfiguration(CACHE_NAME_1, builder_tx.build());
      cacheManager.defineConfiguration(CACHE_NAME_2, builder_no_tx.build());

      Cache<Integer, CustomObject> c = cacheManager.getCache(cache);
      RollingUpgradeManager rum = c.getAdvancedCache().getComponentRegistry().getComponent(RollingUpgradeManager.class);
      long migrated = rum.synchronizeData("hotrod");
      System.out.println("Migrated " + migrated + " entries from " + cache);
      rum.disconnectSource("hotrod");
      c.forEach((key, value) -> System.out.println(key + " -> " + value));

   }
}
