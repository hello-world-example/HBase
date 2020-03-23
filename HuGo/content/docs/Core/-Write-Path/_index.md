# 写路径和优化

HBase 基于 LSM 模式，写的时候是 **顺序写 WAL 和 MemStore 内存**，基本没有随机IO，大部分时候写链路性能高效且平稳

写路径的优化思路大部分有一些几种

- **减少重复操作的资源消耗**：批量操作、本地缓存批量提交(可靠性降低)
- **可靠性 换 性能**：本地缓存批量提交、WAL异步写入 或 跳过
- **读性能 换 写性能**：增大 MemStore 减小 BlockCache
- **提升并行能力**：预分区、扩展集群



## 概述

- Client 通过 Zookeeper 找到 `hbase:meta` 所在的 `RegionServer` > `Region`
- 读取 `hbase:meta` 的信息缓存在 本地，根据 `StartKey` 和 `EndKey` 找到数据所在的  `RegionServer` > `Region`

---

- 直连 `RegionServer` 进行数据的 写入 和 读取
- Client 本地有写缓存，默认自动提交

---

- `RegionServer` 收到数据后，找到其管理的 `Region`，先写 `WAL` 用于服务宕机时的数据恢复，然后写`MemStore` 对内存中的 RowKey 进行整理和排序

---

- `MemStore` 在后台根据各种阈值 `flush` 内存到磁盘生成 `HFile` 真正的持久化

- `Compact` 根据各种阈值对 `HFile` 进行合并，减少随机访问 来提升读取的性能



## 客户端写入

### 写缓冲区

客户端写缓冲区就是在客户端 JVM 里面的缓存机制， 可以把多个Put操作攒到一起通过单个RPC请求发送给服务端， 目的是节省部分网络带来的 IO 消耗。 

#### ~~废弃的 API~~

在老的 API 中的，可通过 `HTable` (`HTableInterface`) 手动设置刷新缓存的大小 、关闭 AutoFlush、手动 `flushCommits`，使用方式如下：

```java
HTableInterface table = (HTableInterface) connection.getTable(TableName.valueOf("test"));
// hbase.client.write.buffer = 2097152
table.setWriteBufferSize(2097152);
// 关闭自动提交
table.setAutoFlush(false);
// 提交
table.flushCommits();
```

1.0.0 之后，该接口被标记为废弃，2.0.0 后类已经被删除，被 `BufferedMutator` 代替。

```java
/**
 * @since 0.21.0
 * @deprecated use {@link org.apache.hadoop.hbase.client.Table} instead
 */
public interface HTableInterface extends Table {
  // @deprecated in 0.96.
  void setAutoFlush(boolean autoFlush);
  // @deprecated in 0.99 since setting clearBufferOnFail is deprecated.  @see BufferedMutator#flush()
  void setAutoFlush(boolean autoFlush, boolean clearBufferOnFail);
  // @deprecated as of 1.0.0. Replaced by {@link BufferedMutator#flush()}
  void flushCommits() throws IOException;
  // @deprecated as of 1.0.0. Replaced by {@link BufferedMutator} and {@link BufferedMutatorParams#writeBufferSize(long)}
  void setWriteBufferSize(long writeBufferSize) throws IOException;
  ...
}
```

 弃用的具体理由是这样的。在1.0版本之前客户端的设计可以简单归纳成以下几点：

- 客户端会维护一个HTablePool， 这是一个存放HTable实例的线程池。
- HTable实例不会每次都创建新的， 而是从HTablePool中尝试获取实例， 获取不到再打开连接。
- 每一个 HTable 都有一个写缓冲区， 用来加速批量操作。

**旧的模式倾向于在内存中维持一个生命周期很长的HTable实例**， 但是这个模式有一些问题：

- 由于 HTable 的生存周期很长， 所以在它之上的写缓冲区（writeBuffer） 生命周期也很长， 如果同时创建了多个HTable， 那势必要消耗大量内存， 这就带来了一些内存管理问题了。 所以不应该一个HTable就带一个writeBuffer
- 如果有多个 HTable 同时存在， 并且活的很久， 那就必须考虑一下线程安全问题了， 但是 HTable 对象又不是线程安全的。 这样需要做的开发工作又增加很多了

**新的模式鼓励大家每次都创建一个 HTable 对象， 用完即释放，这样每一个HTable都是一个轻量级的对象**。

#### BufferedMutator 写缓存

使用方式如下

```java
BufferedMutatorParams params = new BufferedMutatorParams(TableName.valueOf("test"));
// Buffer 大小 默认值通过 hbase.client.write.buffer 属性设置
params.writeBufferSize(2097152);

BufferedMutator mutator = connection.getBufferedMutator(params);
// 提交数据（Append、Delete、Increment、Put 都是 Mutation 的子类）
mutator.mutate(puts);
// 刷新
mutator.flush();
// 关闭
mutator.close();
```



> **目前推荐做法**
>
> - 推荐的做法就是什么都不做。 不需要显式地去调用 `BufferedMutator`
> - 如果你需要批量插入，可以调用批量 put、 get、 delete 方法，每次发送多少数据，可自行控制

### 批量提交

使用方式如下

```java
Table table = connection.getTable(TableName.valueOf(""));
List<Put> batch = new ArrayList<>();
// 无返回值，内部调用 batch 方法
table.put(batch);
// 第二个参数的结果
table.batch(batch, new Object[batch.size()]);
```

批量提交的数据可能属于不同的 Region，batch 会事先的 Client 端进行数据分组，分批次发送到不同的 RegionServer，每组一个 RPC 请求，从而减少 RPC 调用次数。

#### 针对同一行的批量提交

批量提交存在一致性问题，但是 HBase 对同一行数据的操作支持原子操作，假设要对同一行数据同时进行删除又添加的操作，可通过 `table.mutateRow` 方式。

`mutateRow` **只能针对同一行数据操作**，如果设置的 RowKey 不一样，会抛出异常。

```java
Mutation put = ...;
Mutation delete = ...;

RowMutations rowMutations = RowMutations.of(Arrays.asList(delete, put));
table.mutateRow(rowMutations);


RowMutations mutations = new RowMutations(Bytes.toBytes("r1"));
mutations.add(put);
mutations.add(delete);
table.mutateRow(mutations);
```



## 服务端写入

### WAL

WAL  Write Ahead Log 即 预写日志，用来解决宕机之后的数据恢复问题，数据到达 Region 后，先写 WAL 再写 MemStore，WAL 是顺序磁盘操作，如果能提升 WAL 的写入速度，

#### WAL 写入模式

在 Client 端可通过 `put.setDurability(Durability.SYNC_WAL)` 设置 WAL 的写入模式，可选值有以下几种：

```java
public enum Durability {
  /**
   * 默认选项，等同于 SYNC_WAL
   */
  USE_DEFAULT,
  /**
   * 不写 WAL，只写 MemStore，服务宕机后，数据无法回复
   */
  SKIP_WAL,
  /**
   * 异步/延时（后台定时刷新）写 WAL，安全性高于 SKIP_WAL，但是仍然有 数据无法恢复的风险
   * 
   * 刷新间隔通过 hbase.regionserver.optionallogflushinterval 设置，默认 1s
   */
  ASYNC_WAL,
  /**
   * 默认行为，写 WAL 和 MemStore
   * 数据被刷新到文件系统实现，但不一定要刷新到磁盘。
   * 对于HDFS，指把数据刷新到 datanode 的个数（DataNode 的副本机制，不等待副本写入完成）
   */
  SYNC_WAL,
  /**
   * 强制刷磁盘，性能最差
   * 老版本与 SYNC_WAL 的行为是一样的，2.0后 有所区别
   */
  FSYNC_WAL
}
```



#### WAL 相关配置 

- WAL 的主要关注点在 ~~`hbase.regionserver.maxlogs`~~ WAL 日志文件的最大个数，默认 `32`
- 当文件个数超过这个阀值后，会触发 MemStore flush，引发 WAL 日志滚动，旧的日志会被清理掉，从而减少 WAL 的日志大小
- 日志文件个数的**建议值**是： `堆大小 * MemStore占用堆的比例 / WAL 的文件大小`
- WAL大小 = `hbase.regionserver.hlog.blocksize` * `hbase.regionserver.logroll.multiplier`

- 如果堆大小是16G，则 `maxlogs` 的建议值是： 16G * 0.4 / (128M * 0.95) ≈ `54` ，比默认值要大很多



> 从上面计算看出，默认值常常比建议值要小很多，而且计算公式并不被人熟知，给很多人带来操作上的麻烦，所以**目前  ~~`hbase.regionserver.maxlogs`~~ 配置已经被废弃**



- 目前内部的计算公式是 `Math.max(32, 堆大小 * MemStore占用堆的比例 * 2 / WAL 的文件大小)`
- 即 `2 * MemStore 的总大小 / WAL 的文件大小`
- 即 **WAL 日志文件的总大小** =  **两倍的 MemStore 的总大小** ≈ **RegionServer 堆内存的大小**

### MemStore Flush

MemStore 主要对内存中的 KeyValue 进行排序整理，维持数据结构。

当达到一定的阀值，会触发 MemStore 的 flush 操作生成 HFile，从而触发 Compact，频繁的 flush 和 compact 会导致写性能急剧下降，类似于 GC 的 STW。

MemStore 的优化核心在于控制 flush，触发条件大概有一下几种



#### 内存占用达到阀值

- 通过 **`hbase.hregion.memstore.flush.size`** 配置，默认 128m（134217728）

- flush 阀值是定时检查的，每 10s（10000）检查一次，通过 `hbase.server.thread.wakefrequency` 配置
- 如果写入数据量过大、过快，可能会触发 MemStore 的阻塞机制，**禁止写入**





#### ❤ 触发 Store 阻塞机制

- 阀值是  **`hbase.hregion.memstore.flush.size`** * `hbase.hregion.memstore.block.multiplier` （默认 `4`），即 一个检查周期内写入数据量超过 128m * 4 = **512M** 的时候**触发阻塞**
- 达到这个阀值会立即触发一次 flush，同时**阻塞当前 Store 的写请求**



#### RegionServer 的 MemStore 总和达到阀值

- `hbase.regionserver.global.memstore.size.lower.limit` 默认 `0.95`，即占用达到 **全局 MemStore 容量** 的 `95%` 触发刷新
- **`hbase.regionserver.global.memstore.size`** 全局 MemStore 容量，也是百分比，默认 `0.4`， 即**堆内存的 `40%`**



#### ❤ 触发 RegionServer 阻塞机制

- 假设当前 RegionServer 的 堆内存是 16G，默认配置情况下
- 16G * **`hbase.regionserver.global.memstore.size`** * `hbase.regionserver.global.memstore.size.lower.limit` 即 `16*0.4*0.95`=`6.08G` 的时候触发 flush
- 16G * **`hbase.regionserver.global.memstore.size`** 即  `16*0.4`=`6.4G` 的时候**触发 RegionServer 级别的阻塞**



#### WAL 的数量过多

WAL 的数量过多时，可以通过 flush 操作持久化，来减少 WAL 日志的占用。



#### Memstore 达到刷写时间间隔

- 不管以上阀值是否达到，每个一段时间会主动触发一次 flush
- 通过 `hbase.regionserver.optionalcacheflushinterval` 设置，默认 3600000ms 即 1h，如果为 0 则关闭自动刷新功能



#### 手动触发flush

```bash
> help 'flush'
Flush all regions in passed table or pass a region row to
flush an individual region or a region server name whose format
is 'host,port,startcode', to flush all its regions.
For example:

  hbase> flush 'TABLENAME'
  hbase> flush 'REGIONNAME'
  hbase> flush 'ENCODED_REGIONNAME'
  hbase> flush 'REGION_SERVER_NAME'
```

#### 其他

- Region Split
- Region Merge
- ..

### HFile Compaction

每次 MemStore 的 flush 会生成一个 HFile，当 HFile 变多，读取的时候会导致寻址过多，影响查询性能。

为了减少 HFile 的个数（碎片文件的个数），提升查询性能，需要对 HFile 进行 Compact 操作。

**Compaction 时阻塞会阻塞  flush，反向触发 Store 的 block，进而上升到 RegionServer 的 block，这是影响 写入性能的关键，也是最复杂、最难优化的地方。**



#### Minor 和 Major Compaction

- `Minor`：Store 中的 **多个** HFile合并为一个 HFile
  - 移除 TTL 到达的数据
  - 被删除的数据并不移除
  - 触发频率较高
- `Major`：Store 中的 **所有** HFile合并为一个 HFile
  - 被删除的数据 被真正移除
  - 移除 单元格内超过 MaxVersions 的版本数据
  - 触发频率较低， 默认为 7天一次
  - 因为消耗性能较大，不应该让它发生在业务高峰期， **建议手动控制 Major Compaction 的时机**
- 为什么 Minor Compaction 可以移除 TTL 超期的数据，但是无法移除被删除的数据？
  - TTL 的移除条件在数据本身，即通过单条数据的时间即可判断
  - 被删除的数据是一条追加的 墓碑标示，标示和数据可能存在于多个 HFile 文件中，Minor Compaction 并不会一次性把 Store 中所有的 HFile 进行合并，每次只是合并一部分，对于单个 HFile 文件来说，无法得知数据被标记为删除，因为标记可能在其他 HFile 中

## 预分区

Region 数量 少于 RegionServer 数量，说明还没有完全发挥集群的能力，压力不均



## 优化思路小结

- 假如已经到了无法写入、集群经常卡死的地步，就要考虑临时放弃部分读取性能 来提升写性能
- JVM 内存
  - 先看 RegionServer JVM 堆内存是否太小，默认 1G 肯定不够，如果内存充裕，可调整成 16G 甚至更大
  - 大 JVM 内存建议配合 `G1GC` 来减小 FullGC 的 STW 时间，避免暂停时间过长被 ZK 认为宕机，从而触发 RegionServer 的 误杀 和 自杀
  - MemStore 启用 `MSLAB 策略`，MSLAB 与 `G1GC` 的思路类似，搭配可略微减小 GC 的时间
- 增大全局 MemStore 占比，减小 BlockCache 占比 
  - 调大 `hbase.regionserver.global.memstore.size` ，减小 `hfile.block.cache.size`
  - 两者相加不能大于 `0.8`，即 堆内存的 `80%`，否则 HBase 无法启动
- 增大 Store 中 MemStore 大小，`hbase.hregion.memstore.flush.size` 默认 128M
  - 使生成的 HFile 更大，数量更少，减少 Compact
  - 但是如果 512M 了还是阻塞，建议从 HFile 入手
- ❤ 调整 HFile 的 Compact 阀值，`hbase.hstore.blockingStoreFiles` 默认 `7`，可调整成 `上百` 甚至 `上千`
  - 让 MemStore 可以 flush HFile 到磁盘上，即先一并照收，后续再想怎么整理数据
  - HFile 个数变多后，会影响到读取的性能，但起码可保证在业务高峰期系统可正常运行，不会被卡死
  - 等到业务低峰期，定时 触发 Major Compact，对数据进行合并，提升读性能
- ~~调整 WAL 先关阀值，增大 maxlogs，避免达到  maxlogs 指定的文件个数，触发 MemStore flush~~
  - maxlogs = `Math.max(32, 堆大小 * MemStore占用堆的比例 * 2 / WAL 的文件大小)`
  - 调整 JVM 堆内存 和 MemStore 占比 即分子，maxlogs 就会变大
  - 调整 WAL 的文件大小 效果可能不大，因为 WAL 的文件越小，数量增长的越快；WAL 的文件越大，maxlogs 阀值越小
- 根据数据和业务特征，选择合适的 Compact 策略，减小影响范围
- 最后：**扩容基本可以解决所有性能压力问题**，只要压力被分散的足够低和均匀，一般问题都可解决



## Read More

- [HBase最佳实践](https://help.aliyun.com/document_detail/59373.html) > [write写入优化](https://help.aliyun.com/document_detail/59069.html)
- [一场HBase2.x的写入性能优化之旅](https://mp.weixin.qq.com/s/4Ov-fF1nhZ3SA125mV4D2Q)
- [《HBase不睡觉书》](https://book.douban.com/subject/30115996/) > 第8章 再快一点
- [HBase写入优化](https://blog.csdn.net/ukakasu/article/details/80020161)