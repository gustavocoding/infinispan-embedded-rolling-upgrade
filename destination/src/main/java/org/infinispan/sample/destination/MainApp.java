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

   public static final String CACHE_NAME = "cache";

   public static void main(String[] args) throws Exception {
      System.setProperty("infinispan.deserialization.whitelist.regexps", ".*");
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.clustering().cacheMode(CacheMode.DIST_SYNC);
      builder.encoding().key().mediaType(MediaType.APPLICATION_OBJECT_TYPE);
      builder.encoding().value().mediaType(MediaType.APPLICATION_OBJECT_TYPE);
      builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL)
            .transactionManagerLookup(new EmbeddedTransactionManagerLookup())
            .useSynchronization(true)
            .autoCommit(false)
            .lockingMode(LockingMode.PESSIMISTIC);
      RemoteStoreConfigurationBuilder store = builder.persistence().addStore(RemoteStoreConfigurationBuilder.class);
      store.hotRodWrapping(false).rawValues(false)
            .marshaller(GenericJBossMarshaller.class)
            .protocolVersion(PROTOCOL_VERSION_26)
            .remoteCacheName(CACHE_NAME).shared(true)
            .addServer().host("localhost").port(11222);

      GlobalConfigurationBuilder gcb = GlobalConfigurationBuilder.defaultClusteredBuilder();
      gcb.serialization().marshaller(new GenericJBossMarshaller());
      GlobalConfiguration globalConfiguration = gcb.defaultCacheName("default").build();

      DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfiguration, new ConfigurationBuilder().build(), true);

      cacheManager.defineConfiguration(CACHE_NAME, builder.build());

      Cache<Integer, CustomObject> cache = cacheManager.getCache(CACHE_NAME);
      RollingUpgradeManager rum = cache.getAdvancedCache().getComponentRegistry().getComponent(RollingUpgradeManager.class);
      long migrated = rum.synchronizeData("hotrod");
      System.out.println("Migrated " + migrated + " entries");
      rum.disconnectSource("hotrod");
      cache.forEach((key, value) -> System.out.println(key + " -> " + value));
   }
}
