# 架构简介



## Master

Master 可以有多个实现热备，通过 Zookeeper 选举一个为主，其他为备做 Failover。

主要作用有：

- 管理 RegionServer 和 其负载均衡
- 管理和分配 Region
  - Region Split 时分配新的 Region
  - RegionServer 退出时迁移其 Region 到其他 RegionServer
- DDL操作，Namespace 、Table、Family 的增删改
- Namespace 、Table、Family 元数据管理
- ACL 权限控制



## RegionServer

- 管理 Region

- 读写 FS，管理 Table 中的数据

- Client 直连 RegionServer 读写数据，从 Master 中获取元数据，找到 RowKey 所在的 RegionServer

- RegionServer 一般和 HDFS DataNode 在同一台机器上运行，实现数据的本地性

  

## Region

- RowKey 是有序的，HBase 根据 RowKey 来划分和查找 Region



## HLog / WAL

- WAL(Write Ahead Log) 在早期版本中称为 HLog
- 默认写操作会先保证将数据写入这个 Log 文件，才会真正更新 MemStore
- Client 可以通过 `put.setDurability` 设置写入方式，以下几种方式 安全性依次递增，性能依次下降
  - `SKIP_WAL`：不写 WAL，直接写 MemStore
  - `ASYNC_WAL`：异步写 WAL，同时写 MemStore
  - `SYNC_WAL`（`USE_DEFAULT`）： 默认方式，写完 WAL，然后写 MemStore
  - `FSYNC_WAL`：`SYNC_WAL` + flush HFile
- 之前 RegionServer 只有一个 WAL 实例，也就是说一个 RegionServer 的所有 WAL 写都是串行的 
- 在 HBase 1.0 之后，实现了多个 WAL 并行写，以单个 Region 为单位



## Store

- 列族 与 Store 一一对应
- 一个表中有多个列族，则会有多个 Store
- 



## MemStore



## StoreFile / HFile



https://blog.csdn.net/u010916338/article/details/80774554



## 相关配置

- 



## Read More

- Apache HBase ™ Reference Guide
  - [Architecture](http://hbase.apache.org/book.html#_architecture)
- [深入HBase架构解析(一) ](http://www.blogjava.net/DLevin/archive/2015/08/22/426877.html)
- [深入HBase架构解析(二) ](http://www.blogjava.net/DLevin/archive/2015/08/22/426950.html)



