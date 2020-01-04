# 如何使用 Bulk Loading

> 原文地址：[How-to: Use HBase Bulk Loading, and Why](http://blog.cloudera.com/blog/2013/09/how-to-use-hbase-bulk-loading-and-why/)

Apache HBase 为您提供对大数据的 **随机、实时、读/写** 访问，如何有效地将数据迁移到到 HBase 呢？
用户可以尝试通过 **客户端API** 或使用 **`MapReduce`作业`TableOutputFormat`输出格式**，但这些方法存在一些问题，下面会说到。
**HBase bulk loading(批量加载)** 功能更易于使用，并且可以更快地插入相同数量的数据


本博客文章将介绍 bulk loading(批量加载) 功能的基本概念，介绍两种使用场景，并展示了两个例子。

<!--more-->

# Bulk Loading 概述

如果您使用传统方式（API、MapReduce）会有下面这些问题，bulk loading(批量加载) 可能是您的正确选择：

- 您需要调整 MemStore 来获取更大的内存。
- 您需要使用更大的WAL或完全绕过它们。
- 您的压缩和flush队列数百个。
- 您的GC失控，因为您的插入数据量很大。
- 导入数据时，您的延迟超出SLA（Service-Level Agreement，《SRE：Google运维解密》一书中有介绍）

大多数这些症状通常被称为“成长中的痛苦”（growing pains）。使用批量加载可以帮助您避免它们。
 


在HBase中，bulk loading 是准备和加载`HFiles`（HBase自己的文件格式）到`RegionServers`中的过程，
因此**绕过了写入路径**并完全避免了上述那些问题。此过程与ETL类似，如下所示：


## 1.从资源中提取数据，通常是文本文件或其他数据库

HBase不管理数据提取这部分过程。
换句话说，你不能通过直接从 MySQL 读取 `HFiles` 来告诉HBase准备HFile，相反，**你必须以自己的方式来完成数据抽取**。 
例如，您可以在一个表上运行`mysqldump`并将结果文件上传到`HDFS`，或者获取您的Apache HTTP日志文件。
无论如何，在下一步之前，您的数据需要上传到 HDFS。

> 实际上在本地磁盘也可以，如果数据量一台机器可以承受的话；
> Hadoop 的 默认文件系统是本地文件系统 "fs.defaultFS=file:///" 



## 2.将数据转换成HFile

这一步需要MapReduce作业，对于大多数输入类型，您必须自己编写Mapper。
作业将需要发出`row key`作为输出键，以及`KeyValue`，`Put`或`Delete`作为输出值。 
Reducer由HBase负责处理; 您可以使用 **HFileOutputFormat.configureIncrementalLoad（）** 对其进行配置，并执行以下操作：

- 检查表以配置分区器（partitioner）
- 将分区文件上传到群集并将其添加到 DistributedCache（Uploads the partitions file to the cluster and adds it to the DistributedCache）
- 设置减少任务的数量以匹配当前的区域数量（Sets the number of reduce tasks to match the current number of regions）
- 设置**输出键/值类**以匹配`HFileOutputFormat`的要求
- 将reducer设置为执行适当的排序（`KeyValueSortReducer`或`PutSortReducer`）（Sets the reducer up to perform the appropriate sorting ）


在此阶段，将在输出文件夹的每个region创建一个HFile。
请记住，输入数据几乎被完全重写，所以您至少需要比原始数据集的大小多两倍的可用磁盘空间。 
例如，对于`100GB`的`mysqldump`，HDFS中至少应有`200GB`的可用磁盘空间。 您可以在流程结束时删除转储文件。

> 这一步应该是整个 Bulk Loading 中工作量最多的一步，因为需要自定义 MapReduce  Mapper，来解析第一步导入 HDFS 的原始数据
> Mapper 的输出类型一般是 `ImmutableBytesWritable`（row key）, `KeyValue`（列族、列限定符、时间戳、值）
> 最新版可以使用 HFileOutputFormat2 来代替 HFileOutputFormat





## 3.通过告诉RegionServers在哪里找到它们，将文件加载到HBase中

这是最简单的一步。它需要使用`LoadIncrementalHFiles`（通常称为[`completebulkload`](http://hbase.apache.org/book.html#completebulkload)工具），
并通过向其传递一个URL来定位`HDFS`中`HFile`文件，
它将通过服务它的`RegionServer`将每个文件加载到相关区域。 
如果在创建文件后分割区域，该工具将根据新边界自动分割`HFile`。 
这个过程效率不高，所以如果你的表目前正在被其他进程写入，最好在第二步完成后立即加载生成的HFile文件。


下图是这个过程的描述。数据流从原始数据源流向HDFS，RegionServers只需将文件移动到其区域的目录。





# 使用场景

**原始数据集加载**：从另一个数据存储库迁移数据， 所有用户都应考虑此用例。 
首先，你必须学习 并设计表，然后创建表 和 预分割。
切割点必须考虑 row key 分配和 RegionServer 的数量。


这样做的好处是直接写入文件要比通过RegionServer的写入路径（写入MemStore和WAL）并最终刷新，压缩等快得多。
这也意味着您不必为导入数据时繁重的工作负载调整群集，然后再为您正常工作时负载调整它。


**增量加载**：假设您有一些数据集当前由HBase提供服务，但是现在您需要从第三方批量导入更多数据，或者您有一个每晚需要插入数千兆字节的作业。
它可能不像HBase已经提供的数据集那么大，但它可能会影响你的正常服务，使其延迟增加。 
通过正常的写入路径将导致在导入过程中触发很多 flush 和压缩（compactions）等不利影响。
这种额外的IO压力将与您正常的服务查询相竞争。

> Bulk Loading 支持**初始化导入** 和 **增量导入**




# 示例

您可以在您自己的Hadoop集群中使用以下示例，也可以使用 [Cloudera QuickStart VM](https://www.cloudera.com/downloads/quickstart_vms/5-12.html)，它是一个单节点集群镜像，包含一些示例数据。



## 内置的 TSV Bulk Loader

HBase附带一个MR作业，可以读取 指定分割符分隔 的文件并直接输出到HBase表中或创建HFile进行批量加载。

1. 获取示例数据并将其上传到HDFS。
2. 根据预先配置的表，运行ImportTsv作业将文件转换为多个HFile。
3. 准备并加载HBase中的文件。

第一步是打开控制台并使用以下命令获取示例数据：

``` bash
curl -O https://people.apache.org/~jdcryans/word_count.csv
```

该文件是csv格式，只有两列，第一列是单词，第二列是单词出现的个数，没有任何列标题。现在，将文件上传到HDFS：

``` bash
hdfs dfs -put word_count.csv 
```

接下来需要转换文件， 首先你需要设计表格。
为了简单起见，将其称为 "wordcount" - 列族命名为 "f"，row key 是 csv文件第一列单词本身。
创建表格时的最佳做法是根据 rowkey  分布对其进行分割，但对于此示例，我们将创建 五个region 。打开hbase shell：

``` bash
hbase shell 
```

运行下面的命令创建表

``` bash
create 'wordcount', {NAME => 'f'},   {SPLITS => ['g', 'm', 'r', 'w']} 
```


四个分割点将生成五个区域，其中第一个区域以空行键开始。
为了获得更好的分割点，你也可以做一个快速分析，看看这些词是如何真正分布的。

如果您打开的浏览器访问 http//localhost:60010/，您将看到我们新创建的表及其五个 Region 。

> HBase 1.0 之后，web 界面的端口变成了 16010，


现在是时候完成繁重的工作了。 使用“hadoop”脚本在命令行上调用HBase jar将显示可用工具的列表。
我们想要的那个被称为`importtsv`的工具，用法如下：

``` bash
hadoop jar /usr/lib/hbase/hbase-0.94.6-cdh4.3.0-security.jar importtsv
 ERROR: Wrong number of arguments: 0
 Usage: importtsv -Dimporttsv.columns=a,b,c <tablename> <inputdir>
```

我们要使用的命令行如下：

``` bash
hadoop jar /usr/lib/hbase/hbase-0.94.6-cdh4.3.0-security.jar importtsv \
-Dimporttsv.separator=, \
-Dimporttsv.bulk.output=output \
-Dimporttsv.columns=HBASE_ROW_KEY,f:count wordcount word_count.csv
```

> **1.0 之后的使用方式略有不同，如下：**  
> 
> $HADOOP_HOME/bin/hadoop jar $HBASE_HOME/lib/hbase-server-1.2.6.jar importtsv \  
> -Dimporttsv.separator="," \  
> -Dimporttsv.bulk.output="output" \  
> -Dimporttsv.columns="HBASE_ROW_KEY,f:count" \  
> wordcount \  
> word_count.csv
>  
>  
> **请确认有如下环境变量（/etc/profile）**  
>  
> export HBASE_HOME=/opt/websuite/hbase-1.2.6  
> export HADOOP_CLASSPATH="$HADOOP_CLASSPATH:$HBASE_HOME/lib/*"  



以下是不同配置项的简要介绍：

- `-Dimporttsv.separator=,` 指定分隔符是逗号
- `-Dimporttsv.bulk.output=output` 是HFiles写入的相对路径。由于默认情况下虚拟机上的用户是“cloudera”，这意味着这些文件将位于`/user/cloudera/output`中。**如果不使用这个选项，数据将直接写入HBase（传统的MapReduce模式）**。
- `-Dimporttsv.columns=HBASE_ROW_KEY,f:count` `HBASE_ROW_KEY,f:count`是包含在这个文件中的所有列的列表。row key 需要使用全大写的`HBASE_ROW_KEY`字符串来标识, 否则它不会工作。（这里使用是限定词“count”您也可以指定其他任何内容。）


鉴于输入数据量较小，该任务应在一分钟内完成。请注意，会运行五个Reducers，每个区域一个。以下是HDFS上的执行的结果：

``` bash
-rw-r--r--   3 cloudera cloudera         4265   2013-09-12 13:13 output/f/2c0724e0c8054b70bce11342dc91897b
-rw-r--r--   3 cloudera cloudera         3163   2013-09-12 13:14 output/f/786198ca47ae406f9be05c9eb09beb36
-rw-r--r--   3 cloudera cloudera         2487   2013-09-12 13:14 output/f/9b0e5b2a137e479cbc978132e3fc84d2
-rw-r--r--   3 cloudera cloudera         2961   2013-09-12 13:13 output/f/bb341f04c6d845e8bb95830e9946a914
-rw-r--r--   3 cloudera cloudera         1336   2013-09-12 13:14 output/f/c656d893bd704260a613be62bddb4d5f
```

正如你所看到的，这些文件目前属于用户“cloudera”。为了加载它们，我们需要将所有者更改为“hbase”，否则HBase将无权移动这些文件。运行以下命令：

``` bash
sudo -u hdfs hdfs dfs -chown -R  hbase:hbase /user/cloudera/output
```

最后，我们需要使用`completebulkload`工具来指向文件的位置以及我们要加载的表：

``` bash
hbase org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles output wordcount 
```

> 或
> 
> $HADOOP_HOME/bin/hadoop jar $HBASE_HOME/lib/hbase-server-1.2.6.jar completebulkload output wordcount

回到HBase shell中，您可以运行count命令来显示加载了多少行。如果你忘了`chown`，命令会挂起。





# 自定义 MR 任务

TSV批量加载程序适用场景比较局限，由于它将所有内容都解释为字符串，并且不支持特殊的数据格式，所以不得不编写自己的MR作业。 
下面这个例子的 数据包含了 湖人队和凯尔特人队2010年总决赛（第一场）相关的公开Facebook和Twitter消息。, 
你可以在[这里](https://github.com/jrkinley/hbase-bulk-import-example)找到代码。（" Quick Start VM " 带有 git和maven，您可以直接克隆这份代码。）
 

看一下Driver类，最重要的部分如下：

``` java
    job.setMapOutputKeyClass(ImmutableBytesWritable.class);
    job.setMapOutputValueClass(KeyValue.class);
…
	// Auto configure partitioner and reducer
    HFileOutputFormat.configureIncrementalLoad(job, hTable);
```
    
  
首先，Mapper需要输出`ImmutableBytesWritable`类型作为 row key，输出值类型可以是`KeyValue`，`Put`或`Delete`。
第二个片段显示了如何配置`Reducer`, 它实际上完全由`HFileOutputFormat.confgureIncrementalLoad()`处理。


`HBaseKVMapper`类只处理了关心的配置、输出键和值的映射器：

``` java
public class HBaseKVMapper extends Mapper<LongWritable, Text, ImmutableBytesWritable, KeyValue> { 

}
```


为了运行它，您需要使用maven编译项目，并按照README中的链接获取数据文件。（它还包含用于创建表的shell脚本。）
在开始作业之前，请不要忘记将文件上传到HDFS，并将您的类路径设置为 HBase的：

``` bash
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/etc/hbase/conf/:/usr/lib/hbase/*
```


使用类似于此命令行的命令行启动作业：

``` bash
hadoop jar hbase-examples-0.0.1-SNAPSHOT.jar
com.cloudera.examples.hbase.bulkimport.Driver -libjars
/home/cloudera/.m2/repository/joda-time/joda-time/2.1/joda-time-2.1.jar,
/home/cloudera/.m2/repository/net/sf/opencsv/opencsv/2.3/opencsv-2.3.jar
"RowFeeder for Celtics and Lakers Game" 1.csv output2 NBAFinal2010
```


 正如你所看到的，工作的依赖关系必须单独添加。最后，您要先更改输出文件的所有者，然后运行`completebulkload`工具来加载文件：

``` bash
sudo -u hdfs hdfs dfs -chown -R hbase:hbase/user/cloudera/output2
hbase org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles output2 NBAFinal2010 
```


# 潜在的问题


## 最近删除的数据重现
当通过批量加载插入`Delete`时发生此问题，主要是因为 `Put`仍在`MemStore`中，这是用进行major压缩。 
当删除处于HFile中时，数据将被视为已删除，但一旦在压缩过程中将其删除，Put将再次变为可见。, 
如果您有这样的用例，请考虑配置您的列族以使用`KEEP_DELETED_CELLS`将已删除的单元格保留在`shell`或`HColumnDescriptor.setKeepDeletedCells()`中。


## 批量加载的数据不能被其他批量加载覆盖
当在不同时间加载的两个批量加载的HFile尝试在同一单元中写入不同的值时，会出现此问题，这意味着它们具有相同的行键，系列，限定符和时间戳。 
结果是第一个插入的值将被返回而不是第二个。这个bug将在`HBase 0.96.0`和`CDH 5`（下一个CDH主要版本）中得到解决，
并且`HBASE-8521`正在为`0.94`分支和`CDH 4`完成工作。


## 批量加载触发 major压缩
当您**执行增量批量加载时，会出现此问题**，并且有足够的批量加载文件来触发 minor 压缩（默认阈值为3）。
HFiles的序列号被设置为0，所以当RegionServer选择文件进行压缩时，它们会首先被拾取，并且由于这个bug，它还会选择所有剩余的文件。
这个问题将严重影响那些已经拥有很大数据 region（数GB）或经常批量加载（每隔几小时或更少）的情况，因为大量数据将被压缩。 
`HBase 0.96.0`有适当的修复，`CDH 5`也是如此; `HBASE-8521`解决了`0.94`中的问题，因为批量加载的HFile现在被分配了正确的序列号。
HBASE-8283可以通过`hbase.hstore.useExploringCompation`在`0.94.9`和`CDH 4.4.0`之后启用，以便通过更智能的压缩选择算法来缓解此问题。


## 批量加载的数据不会被复制
当批量加载绕过写入路径时，WAL不会被写入。 
复制通过读取WAL文件来工作，因此它不会看到批量加载的数据 - 使用`Put.setWriteToWAL(true)`时也是如此。
处理这种情况的一种方法是将原始文件或HFile发送到其他群集，并在那里进行其load处理。

    
# 结论

本博文的目标是向您介绍Apache HBase批量加载的基本概念。
我们解释了这个过程是如何进行ETL的，并且它比使用普通API更适合大数据集，因为它绕过了写路径。
这两个例子包含了如何将简单的TSV文件批量加载到HBase以及如何为其他数据格式编写您自己的Mapper。
 

# Read More

- [HBase 写优化之 BulkLoad 实现数据快速入库](https://my.oschina.net/leejun2005/blog/187309)
    - > 通常 `MapReduce` 在写HBase时使用的是 `TableOutputFormat` 方式，在reduce中直接生成`Put`对象写入`HBase`， 该方式在大数据量写入时效率低下（HBase会频繁进行`flush`，`split`，`compact`等大量IO操作），并对HBase节点的稳定性造成一定的影响（GC时间过长，响应变慢，导致节点超时退出，并引起一系列连锁反应）。
      > 而HBase支持 `bulk load` 的入库方式，它是利用hbase的数据信息按照特定格式存储在hdfs内这一原理，直接在HDFS中生成持久化的HFile数据格式文件，然后上传至合适位置，即完成巨量数据快速入库的办法。
      > 配合mapreduce完成，高效便捷，而且不占用region资源，增添负载，在大数据量写入时能极大的提高写入效率，并降低对HBase节点的写入压力。
- [MapReduce生成HFile入库到HBase及源码分析](http://blog.pureisle.net/archives/1950.html)

