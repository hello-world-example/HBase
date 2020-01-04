# HBase 配置简介

获取 HBase 可配置的选项通常有以下方法：

- `org.apache.hadoop.hbase.HConstants` 常量类
- `hbase-common-x.x.x.jar` 根目录下的 `hbase-default.xml` 配置文件
- HMaster Web UI Configuration: [http://localhost:16010/conf](http://localhost:16010/conf)



## Core / Install

**以下是让 HBase 启动和运行的重要配置**


### hbase.tmp.dir 

本地文件系统上的临时目录

默认 `${java.io.tmpdir}/hbase-${user.name}`（`/tmp/hbase-${user.name}`）, 系统重启会自动清除


### `hbase.rootdir`

HBase 的数据保存目录。
如果是本地文件系统，必须是全路径；如果是 HDFS `hdfs://namenode.example.org:9000/hbase`

默认 `${hbase.tmp.dir}/hbase`


### hbase.fs.tmp.dir

默认文件系统（HDFS） 上用于**暂存临时数据**的目录

默认 `/user/${user.name}/hbase-staging`


### hbase.bulkload.staging.dir

默认文件系统（HDFS）中的**暂存批量加载临时数据**目录

默认 `${hbase.fs.tmp.dir}`


### `hbase.cluster.distributed`

是否在集群模式下运行。
独立模式下的值为false，分布式模式下为true。 **如果为false，将在一个JVM中一起运行所有HBase和ZooKeeper守护进程**

默认 `false`


### `hbase.zookeeper.quorum`

用逗号分隔的 ZooKeeper 服务器的列表。 例如`host1.mydomain.com，host2.mydomain.com，host3.mydomain.com`。
默认情况下，**独立模式**和**伪分布式模式** 下为localhost。
对于完全分布式模式，应该将其设置为ZooKeeper服务器的完整列表。
如果对`hbase-env.sh`中`HBASE_MANAGES_ZK`的设为`true`，HBase 会把 ZooKeeper 作为群集的一部分 启动（/停止）。
在客户端，常与`hbase.zookeeper.clientPort`（Zookeeper 端口）配置放在一起使用，并将其作为`connectString`参数传递给zookeeper构造函数

默认 `localhost`





## Master


### `hbase.master.port`

HBase Master应绑定的端口。

默认 `16000`</value>



### `hbase.master.info.port`

HBase Master Web UI的端口。如果您不想运行UI实例，请将其设置为`-1`。

默认 `16010`




### hbase.master.logcleaner.plugins

清理日志的插件列表，逗号分隔，被`LogService`调用的`LogCleanerDelegate`，可以自定义，顺序执行，清理`WAL`和`HLog`

默认 `org.apache.hadoop.hbase.master.cleaner.TimeToLiveLogCleaner`



### hbase.master.logcleaner.ttl

HLog在`.oldlogdir`目录中生存的最长时间，过期则被Master起线程回收，默认是`600000`；




### hbase.master.hfilecleaner.plugins

HFile的清理插件列表，逗号分隔，被`HFileService`调用，可以自定义，

默认 `org.apache.hadoop.hbase.master.cleaner.TimeToLiveHFileCleaner`



### hbase.master.catalog.timeout

`Catalog Janitor` 从 master 到 META 的超时时间。Janitor是定时的去META扫描表目录，来决定回收无用的regions

默认 `600000`



### hbase.master.infoserver.redirect

Master是否监听Master Web UI端口（`hbase.master.info.port`）并将请求重定向到由Master和RegionServer共享的Web UI服务器

默认 `true`


























## RegionServer


### `hbase.regionserver.port`

RegionServer 绑定的端口

默认 `60020`



### `hbase.regionserver.info.port`

RegionServer 的web界面端口，-1取消界面

默认 `60030`



#### hbase.regionserver.handler.count

在 RegionServers 上启动 RPC Listener 实例的数。Master 使用相同的属性来计算主处理程序的数量

默认 `30`



### hbase.ipc.server.callqueue.handler.factor

确定处理队列数量的因子。值为0表示在所有处理程序之间共享单个队列。值为1意味着每个处理程序都有自己的队列。

默认 `0.1`



### hbase.ipc.server.callqueue.read.ratio

将处理队列分成 读/写 队列，值应该在0.0到1.0之间，该值将乘以呼叫队列的数量。
值为0表示不分割处理队列，这意味着读取和写入请求都会被推送到同一组队列中。
低于0.5的值意味着读队列比写队列少。
值为0.5意味着将有相同数量的读写队列。
大于0.5的值意味着读队列比写队多对。
值为1.0意味着所有的队列都用于分派读取请求，只有一个队列用于写。

假设处理队列的总数为10，则：
- read.ratio为0意味着：10个队列将同时包含读取/写入请求。
- 0.3的read.ratio表示：3个队列将只包含读取请求，7个队列将只包含写入请求。 
- 0.5的read.ratio表示：5个队列将只包含读取请求，5个队列将只包含写入请求。 
- 0.8的read.ratio意味着：8个队列将只包含读取请求，2个队列将只包含写入请求。 
- 1的read.ratio表示：9个队列将只包含读取请求，1个队列将仅包含写入请求。

默认 `0`


​    
### hbase.ipc.server.callqueue.scan.ratio

该属性将读取调用队列分为 **小读取和长读取**队列。
低于0.5的值意味着 短读取队列的长读队列少。
值为0.5意味着将有相同数量的短读取和长读取队列。
大于0.5的值意味着将有更多的长读队列比短读队列值
0或1表示使用同一组队列进行获取和扫描

假设读取调用队列的总数为8：
- scan.ratio为0或1意味着：8个队列将包含长读请求和短读请求。 
- 0.3的scan.ratio表示：2个队列将只包含长读请求，而6个队列将只包含短读请求。 
- 0.5的scan.ratio表示：4个队列只包含长读请求，4个队列只包含短读请求。 
- 0.8的scan.ratio意味着：6个队列将只包含长读请求，2个队列将只包含短读请求。

默认 `0`



### hbase.regionserver.msginterval

RegionServers 向 Master 发消息的间隔

默认 `3000` 毫秒



### `hbase.regionserver.logroll.period`

无论有多少次修改，我们将在此期间滚动提交日志

默认 `3600000` 毫秒 1小时




### hbase.regionserver.optionalcacheflushinterval

一个edit版本在内存中cache的最长时长，设置为0的话则禁止自动flush

默认 `3600000` 毫秒


### `hbase.regionserver.region.split.policy`

split 政策决定了如何拆分 Region。当前可用的各种拆分策略是：


- `DisabledRegionSplitPolicy` 禁用切割
- `ConstantSizeRegionSplitPolicy` 仅仅当region大小超过常量值（`hbase.hregion.max.filesize`大小）时，才进行拆分
    - `IncreasingToUpperBoundRegionSplitPolicy` 根据公式`min(r^2*flushSize，maxFileSize)`确定split的maxFileSize，其中`r`为该表在线region个数，maxFileSize由`hbase.hregion.max.filesize`指定
        - `SteppingSplitPolicy`  根据公式`tableRegionsCount == 1  ? hbase.increasing.policy.initial.size : getDesiredMaxFileSize()`确定split的maxFileSize
        - `DelimitedKeyPrefixRegionSplitPolicy`  保证以分隔符前面的前缀为splitPoint，保证相同RowKey前缀的数据在一个Region中
        - `KeyPrefixRegionSplitPolicy` 保证具有相同前缀的row在一个region中（**要求设计中前缀具有同样长度**）。指定rowkey前缀位数划分region，通过读取table的`prefix_split_key_policy.prefix_length`属性，该属性为数字类型，表示前缀长度，在进行split时，按此长度对splitPoint进行截取。**此种策略比较适合固定前缀的rowkey**。当table中没有设置该属性，或其属性不为Integer类型时，指定此策略效果等同与使用`IncreasingToUpperBoundRegionSplitPolicy`


默认 `org.apache.hadoop.hbase.regionserver.IncreasingToUpperBoundRegionSplitPolicy`

> [HBase笔记：Region拆分策略](https://yq.aliyun.com/articles/25849)


### `hbase.regionserver.regionSplitLimit`

限制 Region 数量，达到之后不再发生切割。这不是对 Region 数量的严格限制，而是作为 Region Server 在一定限制后停止分裂的指导原则。

默认 `1000`









































## HRegion

### `hbase.hregion.memstore.flush.size`

Memstore写磁盘的flush阈值，超过这个大小就flush，值由每个`hbase.server.thread.wakefrequency`运行的线程检查

默认 `134217728` (128 M)


### `hbase.hregion.max.filesize`

HStoreFile的最大尺寸，换句话说，当一个region里的列族的任意一个HStoreFile超过这个大小，那么region进行split，

默认 `10737418240` （10 G ）



### hbase.hregion.percolumnfamilyflush.size.lower.bound

如果使用了`FlushLargeStoresPolicy`，那么每当我们达到的 总的memstore限制 时，我们就会找出所有其memstore超过此值的列族，
并且只保留其中的memstore低于此限制的其他列。如果没有一个列族的memstore大小超过这个数量，那么所有的memstore都将被刷新（就像往常一样）。
该值应该小于总内存阈值（`hbase.hregion.memstore.flush.size`）的一半

默认 `16777216` （16M）



### hbase.hregion.preclose.flush.size

如果在关闭 Region 时，某个 Region 内的 memstore 大小超过此大小，会先运行`pre-flush`以清除 memstore，然后再放置Region关闭标记并使Region脱机。
关闭时，在关闭标志下运行刷新以清空内存。在此期间，该Region处于离线状态，没有进行任何写入。如果memstore内容很大，则此刷新可能需要很长时间才能完成。
这个`pre-flush`意味着清理大部分的 memstore，然后再放置关闭标志并将该区域置于离线状态，这样在关闭标志下运行的flush就很快了。

默认 `5242880` (5M)







### `hbase.hregion.majorcompaction`

对一个`Region`的所有`HStoreFile`进行`major compact`的时间周期，

默认 `604800000` 毫秒（7天）；


































## HStore

### `hbase.hstore.blockingStoreFiles`

如果任何一个Store中存储的StoreFiles数量超过此数量（**每次刷新MemStore时将写入一个StoreFile**），则会更新此HRegion，直到压缩完成 或者 已超出`hbase.hstore.blockingWaitTime`

默认 `10`



### `hbase.hstore.blockingWaitTime`

HRegion在达到`hbase.hstore.blockingStoreFiles`定义的StoreFile限制后，开始compaction，如果时间超过该值，即使压缩尚未完成，HRegion也会停止阻塞更新。

默认 `90000` 毫秒



### `hbase.hstore.compaction.max`

每个minor compaction的HStoreFile个数上限，

默认 `10`



### `hbase.hstore.compaction.kv.max`

在`flushing`或者`compacting`时可以读取多少个KeyValue并批量写入，如果有大的 KeyValue 或者 OOME 的话则配置一个小的值，如果行数多且小则配置大值

默认 `10`





























 



## HFile


### `hfile.block.cache.size`

要分配给 HFile和StoreFile 使用的最大堆（-Xmx设置）的百分比。设置为0禁用，但不建议; 您至少需要足够的缓存才能保存存储文件索引

默认 `0.4`，最大堆配置的 40％ 用来作为缓存



### hfile.index.block.max.size

当多级块索引中的 叶子块、非叶子块、根级索引块的大小增长到此大小时，块将被写出并启动一个新块

默认 `131072`











## Client





### `hbase.client.write.buffer`

HTable客户端写缓存区的默认字节大小。
该值越大消耗的内存也就越多，由于服务器端也需要消耗内存来处理传入的数据，客户端与服务器端都会消耗更多的内存
较大的缓冲区有助于减少RPC调用的次数。例如，服务器端的内存消耗大概等于 `hbase.client.write.buffer` * `hbase.regionserver.handler.count` 的值。

默认 `2097152`（2M）


### `hbase.client.pause`

在 HBase 操作失败掉的时候进行 暂停 的时间长度

默认 `100` 毫秒



### `hbase.client.retries.number`

操作失败时的重试次数，结合上一个指标一起来控制总的重试时间

默认 `35`


### `hbase.client.max.total.tasks`

一个HTable实例可以提交给集群的最大并发任务数

默认 `100`







### hbase.client.keyvalue.maxsize

设置 KeyValue实例的最大大小，这是为了协助设置存储文件中单个条目存储的上限。
这种做法有利于避免单条数据过大不能被拆分的现象，如果用户想绕开这个检查，可以将这个参数设置为0或更少。

默认 `10485760`（10M）


### `hbase.client.scanner.max.result.size`

调用扫描器的 `next` 方法时返回的**最大字节数**。请注意，**当单个行大于此限制时，行仍然完全返回**。默认值是2MB，这对1ge网络很有用。有了更快和/或更高的延迟网络，这个值应该增加。

默认 `2097152` 2M


- [HBase中Scan类属性maxResultSize的说明](https://my.oschina.net/psuyun/blog/375637)



### `hbase.client.scanner.caching`


扫描器调用next方法的时候发现本地客户端内存的数据已经取完，就会向服务端发起请求，该值就是扫描器调用next方法一次性从服务器端返回的**最大行数**。
该值越大，扫描器整体的返回速度就越快，但同时依赖的内存也就越多，并且当请求的数据没有在内存中命中的话，next方法的返回时间可能会更长，
因此要避免这个时间长于扫描器超时的时间，即`hbase.regionserver.lease.period`。
该配置与`hbase.client.scanner.max.result.size`一起使用，以便高效地使用网络。

- 默认值：`Integer.MAX_VALUE` （2G）



### `hbase.client.scanner.timeout.period`   

scanner的超时时间

默认 `60000`毫秒 （1分钟）


- [HBase基础知识(8):扫描操作之缓存与批量处理](http://blog.csdn.net/pangjiuzala/article/details/48023129)
- [由hbase.client.scanner.caching参数引发的血案](http://blog.csdn.net/rzhzhz/article/details/7536285)
- [HBase性能优化方法总结（三）：读表操作](https://www.cnblogs.com/panfeng412/archive/2012/03/08/hbase-performance-tuning-section3.html)


### hbase.client.max.perserver.tasks

一个HTable实例给一台regionserver提交的最大并发任务数

默认 `5`



### hbase.client.max.perregion.tasks

客户端保持到单个 Region 的最大并发连接数。也就是说，如果已经有`hbase.client.max.perregion.tasks`个线程操作该 Region，则新的操作将不会发送到该Region，直到某些写入完成

默认 `1`


















## Miscellaneous


### hbase.balancer.period

在 master 节点中运行 region 负载均衡器的周期

默认 `300000`毫秒 （5分钟）



























## Read More

- [7. Default Configuration](http://hbase.apache.org/book.html#config.files)
- [Hbase配置项简介](http://blog.sina.com.cn/s/blog_6277623c0102v3jy.html)
- [HBase 默认配置](http://eclecl1314-163-com.iteye.com/blog/1474286)
