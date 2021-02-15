# Infinispan Embedded Rolling Upgrade

## Info 
Showcase for migration of embedded clusters using Hot Rod. 

An embedded cluster with version ```8.2.8``` stores caches
as POJOs, and has an embedded Hot Rod server that is used by a second cluster running ```11.0.x``` to migrate data without downtime.

:warning: Java 8 should be used to run the processes!

## Running the source cluster

Start one of more nodes with: 

```
cd source/
mvn exec:java
```

To avoid port conflict in successive nodes, use the ```offset``` JVM property, e.g.: 

```mvn clean install -Doffset=1000 exec:java```

## Running the target cluster

Start the target node with:

```
cd destination/
mvn -Dcache=cache1 exec:java
```

The ```cache``` system property indicates which cache should be migrated, either ```cache1``` or ```cache2```. 
Only the cache chosen will be started in the node, you can check the output log containing lines:

```
14:31:20,908 DEBUG (org.infinispan.sample.destination.MainApp.main()) [CacheImpl] Started cache cache1 on node-14851
```

After migration, it will migrate the data and print the contents of the cache.