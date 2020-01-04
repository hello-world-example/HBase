# HBase 列族配置


创建一个测试表（`test`），列族名为 `f`
``` bash
hbase> create 'test', {NAME => 'f'}
```

默认属性如下：
``` bash
hbase> describe 'test'

{
 NAME => 'f', 
 BLOOMFILTER => 'ROW', 
 VERSIONS => '1', 
 MIN_VERSIONS => '0', 
 KEEP_DELETED_CELLS => 'FALSE', 
 DATA_BLOCK_ENCODING => 'NONE', 
 COMPRESSION => 'NONE', 
 TTL => 'FOREVER', 
 BLOCKCACHE => 'true', 
 BLOCKSIZE => '65536', 
 IN_MEMORY => 'false',
 REPLICATION_SCOPE => '0'
}
```
<!--more--->

# 基本属性

## BLOOMFILTER(布隆过滤器)

**布隆过滤器 可以判断数据不存在**，当判断结果存在时，数据可能真的存在，也可能不存在。可选的值有 三个 `NONE`, `ROW` (default), `ROWCOL`。

当开启 布隆过滤器 时，**可以判断要查询的数据不存在在表中**，从而避免查表。

行级（`ROW`）布隆过滤器在数据块里**检查特定行键是否不存在**，列标识符级（`ROWCOL`）布隆过滤器**检查行和列标识符联合体是否不存在**。`ROWCOL`布隆过滤器的开销高于`ROW`布隆过滤器。





## VERSIONS(Cell数据版本)

0.96版本默认是3个， 0.98版本之后是1， 要根据业务来划分，版本是历史记录，版本增多意味空间消耗。

插入数据的时候，版本默认是当前时间；查询的时候可以指定要获取的版本个数 `get 'test', 'rk1',  { COLUMN => 'f',  VERSIONS => 100}`；

获取多个版本的时候，多个数据是按照 时间戳倒序排序，也可以通过这个特性，来保存类似于事件发生的数据，查询时间历史的时候，拿出来的数据是按照时间排好序，如果要拿最新的事件，不指定版本即可。

版本的时间戳，也可以自定义，不使用默认生成的时间戳，可以自己指定业务相关的ID


## MIN_VERSIONS(最少保留的版本数)

如果所有的版本都超期了，至少要保留`MIN_VERSIONS`个版本




## KEEP_DELETED_CELLS(保留删除的单元格)

HBase 的`delete`命令，并不是真的删除数据，而是设置一个标记（`delete marker`）。用户在检索数据的时候，会过滤掉这些标示的数据。
该属性可以设置为 `FALSE`（默认）、`TRUE`、`TTL`。API 的注释里对这三种属性有详细描述：如下

``` java
package org.apache.hadoop.hbase;

public enum KeepDeletedCells {

  /** 不保留删除的单元格. */
  FALSE,
  
  /**
   * 删除的单元格会保留，超期（TTL）或者数据版本数超过 VERSIONS 设置的值 才会被删除。
   * 如果没有指定TTL或没有超出VERSIONS值，则会永久保留它们。
   */
  TRUE,
  
  /**
   * 超期（TTL）才会删除
   * 当TTL与MIN_VERSIONS结合使用时，会删除过期后的数据，但是同时会保留最少数量的版本。
   */
  TTL;
}
```

> [41. Keeping Deleted Cells](http://hbase.apache.org/book.html#cf.keep.deleted)


major_compact

``` bash
alter 'test', NAME => 'f', VERSIONS => 100

put 'test', 'k1' , 'f:c', 123
put 'test', 'k1' , 'f:c', 456
scan 'test',{VERSIONS => 100}
alter 'test' , NAME => 'f', KEEP_DELETED_CELLS => true



```




## COMPRESSION(压缩)

数据压缩是 HBase 提供的一个特性，HBase 在写入数据块到 HDFS 之前会首**先对数据块进行压缩，再落盘**，从而可以减少磁盘空间使用量。
而在读数据的时候首先从HDFS中加载出block块之后**进行解压缩，然后再缓存到BlockCache**，最后返回给用户。

写路径和读路径分别如下

**写路径**： _Finish DataBlock_  -->  _Encoding KVs_  -->  **`Compress DataBlock`**  -->  _Flush_
**读路径**： _Read Block From Disk_  -->  **`DeCompress DataBlock`**  -->  _Cache DataBlock_  -->  _Decoding Scan KVs_


压缩可以**节省空间**，读写数据会**增加CPU负载**，HBase目前提供了三种常用的压缩方式： GZip, LZO, **`Snappy`**；
**`Snappy`** 的压缩率最低，但是编解码速率最高，对CPU的消耗也最小，目前一般建议使用 **`Snappy`**。


> [HBase最佳实践－列族设计优化](http://blog.csdn.net/javastart/article/details/51820212)




## DATA_BLOCK_ENCODING（编码）

除了数据压缩之外，HBase还提供了数据编码功能。
和压缩一样，数据在落盘之前首先会对KV数据进行编码；但又**和压缩不同，数据块在缓存前并没有执行解码**。
因此即使后续**命中缓存的查询是编码的数据块，需要解码后才能获取到具体的KV数据**。
和不编码情况相比，相同数据block快占用内存更少，即**内存利用率更高**，但是读取的时候需要解码，又不利于读性能；
在内存不足的情况下，可以压榨 CPU 字段，换区更多的缓存数据；
HBase目前提供了四种常用的编码方式：  **`Prefix_Tree`**、 Diff 、 Fast_Diff 、Prefix 


写路径和读路径分别如下：

**写路径**： _Finish DataBlock_  -->  **`Encoding KVs`**  -->  _Compress DataBlock_  -->  _Flush_
**读路径**： _Read Block From Disk_  -->  _DeCompress DataBlock_  -->  _Cache DataBlock_  -->  **`Decoding Scan KVs`**


> [HBase-0.96中新BlockEncoding算法-PREFIX_TREE压缩的初步探究及测试](http://zjushch.iteye.com/blog/1843793)









## TTL(存活时间)

当数据记录一段时间想删除掉，设置一个时间，超过后会被设置删除标记，单位是 s（秒），超过这个时间数据的就会在下一次大合并中被删除





## BLOCKCACHE(数据块缓存的配置)

如果 **经常顺序访问（scan）** 或很少被随机访问，可以关闭列族的缓存；列族缓存默认是 true


## BLOCKSIZE(数据块大小配置)
默认是64K（65536，单位是字节）； 跟 HDFS 不是一个概念，仅仅是 HBase 内部的一个属性；数据块越小，索引越大，占用内存也越大；

 
> 随着BlockSize的增大，系统随机读的吞吐量不断降低，延迟不断增大。64K大小比16K大小的吞吐量大约降低13%，延迟增大13%。
> 同样的，128K大小比64K大小的吞吐量降低约22%，延迟增大27%。
> **如果业务请求以Get请求为主，可以考虑将块大小设置较小；如果以Scan请求为主，可以将块大小调大**
> 
> [HBase最佳实践－列族设计优化](http://blog.csdn.net/javastart/article/details/51820212)



## IN_MEMORY(激进缓存的配置)

可以选择一个列族赋予更高的优先级缓存, 激进缓存，表示优先级更高，IN_MEMORY 默认是 false







## REPLICATION_SCOPE(复制)
 
通过 HBase 的 replication 功能可以实现集群间的相互复制。

> [Hbase Replication 介绍](http://lib.csdn.net/article/hbase/43717)
> [150. Cluster Replication](http://hbase.apache.org/book.html#_cluster_replication)






 
# Read More

- [**HBase API 常量**](http://hbase.apache.org/apidocs/constant-values.html)
- [113. Schema Design](http://hbase.apache.org/book.html#perf.schema)
    - [113.4. Bloom Filters](http://hbase.apache.org/book.html#schema.bloom)
    - [41. Keeping Deleted Cells](http://hbase.apache.org/book.html#cf.keep.deleted)
 

 
 

 

