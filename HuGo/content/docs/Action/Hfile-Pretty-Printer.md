# 通过 “HFile Tool” 查看 HFile文件内容

查看 HFile 内容的文本版本, 你可以使用`org.apache.hadoop.hbase.io.hfile.HFile`工具。输入以下命令查看使用帮助。

``` bash
$ hbase org.apache.hadoop.hbase.io.hfile.HFile

usage: HFile [-a] [-b] [-e] [-f <arg> | -r <arg>] [-h] [-k] [-m] [-p] [-s] [-v] [-w <arg>]

    -a,--checkfamily         Enable family check
    -b,--printblocks         Print block index meta data
    -e,--printkey            Print keys
    -f,--file <arg>          File to scan. Pass full-path; e.g. hdfs://a:9000/hbase/hbase:meta/12/34
    -h,--printblockheaders   Print block headers for each block.
    -k,--checkrow            Enable row order check; looks for out-of-order keys
    -m,--printmeta           Print meta data of file
    -p,--printkv             Print key/value pairs
    -r,--region <arg>        Region to scan. Pass region name; e.g. 'hbase:meta,,1'
    -s,--stats               Print statistics
    -v,--verbose             Verbose output; emits file and meta data delimiters
    -w,--seekToRow <arg>     Seek to this row and print all the kvs for this row only
```

<!-- more -->

## HFile 文件 在 HDFS 上的位置

``` text
/hbase                                  # hbase.rootdir 配置的 HDFS 根路径
    /data                               # 保存表数据
        /<namespace>                    # 命名空间
            /<Table>                    # 表名
                /<Region>               # 表分割的 region
                    /<ColumnFamily>     # 表的列族
                        /<StoreFile>    # Region 下 表列族的 StoreFile（HFile）
```

`-f` 参数的看起来像这样

``` bash
-f hdfs://s01.hbase.kail.xyz:8020/hbase/data/default/test_table/9983d7464bcf4488a0b89a51711048b5/f1/e28e891e61554099b680da55b6f8aadd
```

`org.apache.hadoop.hbase.io.hfile.HFile` 内部实际上直接调用 `HFilePrettyPrinter` 的 `main`方法；
通过 `FileSystem.get`方法，判断 `hdfs://` schema，从而返回具体的 `FileSystem` 实现，所以 HFile 不一定在 HDFS 上，本地文件也可以。



## 校验相关


###  -k,--checkrow

HBase的数据是按照字典顺序存储的，该参数会 检查行顺序，查看是否有乱序的行。通过以下方式进行检查：
``` java
if (CellComparator.compareRows(pCell, cell) > 0) {
    System.err.println("WARNING, previous row is greater then current row\n\tfilename -> " + file + "\n\tprevious -> "
      + CellUtil.getCellKeyAsString(pCell) + "\n\tcurrent  -> " + CellUtil.getCellKeyAsString(cell));
}
```

###  -a,--checkfamily

1. 检查 cell列族名 与 `-f` 传入的 文件路径中的列族名是否一致
2. 与 `-k` 一样，检查 列族是否有乱序的情况



## 打印

###  -m,--printmeta

打印元数据信息，包括：起始键、结束键、中间键、平均Key长，数据量、数据大小 等等

``` 
Block index size as per heapsize: 57040
reader=hdfs://s01.hbase.kail.xyz:8020/hbase/data/default/test_table/9983d7464bcf4488a0b89a51711048b5/f1/e28e891e61554099b680da55b6f8aadd,
    compression=none,
    cacheConf=CacheConfig:disabled,
    firstKey=00000002/f1:200/1516688310337/Put,
    lastKey=50092252/f1:200/1498108990811/Put,
    avgKeyLen=28,
    avgValueLen=149,
    entries=279775,
    length=52426652
Trailer:
    ...
Fileinfo:
    BLOOM_FILTER_TYPE = ROW
    ...
Mid-key: ...
Bloom filter:
   ...
Delete Family Bloom filter:
    Not present

```


###  -e,--printkey

打印出所有的键(行键/列族:列限定符/时间戳/Put/vlen=数据长度/seqid=0)

``` text
... ...
K: 50091132/f1:200/1476519526747/Put/vlen=138/seqid=0
K: 50091132/f1:200/1476519526310/Put/vlen=141/seqid=0
K: 50091582/f1:200/1519364386080/Put/vlen=145/seqid=0
K: 50092052/f1:200/1495776572214/Put/vlen=157/seqid=0
K: 50092052/f1:200/1495774348548/Put/vlen=147/seqid=0
K: 50092052/f1:200/1495774348214/Put/vlen=157/seqid=0
K: 50092252/f1:200/1498108990911/Put/vlen=130/seqid=0
K: 50092252/f1:200/1498108990811/Put/vlen=145/seqid=0
... ...
```


###  -p,--printkv

除了 `-e` 输出的信息，还输出 value


###  -b,--printblocks

打印出每个数据块的大小，偏移量，保存的起始行键等。


###  -h,--printblockheaders

打印出每个数据块的头信息，下面摘录出一个数据块的头

```
HFileBlock [ 
    fileOffset=65682 
    headerSize()=33 
    blockType=DATA 
    onDiskSizeWithoutHeader=65718 
    uncompressedSizeWithoutHeader=65698 
    prevBlockOffset=0 
    isUseHBaseChecksum()=true 
    checksumType=CRC32C 
    bytesPerChecksum=16384 
    onDiskDataSizeWithHeader=65731 
    getOnDiskSizeWithHeader()=65751 
    totalChecksumBytes()=20 
    isUnpacked()=true 
    buf=[ java.nio.HeapByteBuffer[pos=0 lim=65751 cap=65784] ] 
    dataBeginsWith=\x00\x00\x00\x94\x00\x0800055262\x06f120\x00\x00\x01^
    fileContext=HFileContext [ 
            usesHBaseChecksum=true 
            checksumType=CRC32C 
            bytesPerChecksum=16384 
            blocksize=65536 
            encoding=NONE 
            includesMvcc=false 
            includesTags=false 
            compressAlgo=NONE 
            compressTags=false 
            cryptoContext=[ 
                cipher=NONE 
                keyHash=NONE 
            ] 
        ] 
]
```


## 其他

### -s,--stats

打印统计信息

### -w,--seekToRow <arg>

只遍历某一行


### -r,--region <arg> 

以上所有参数都必须配合 `-f` 参数使用，来打印某个 HFile 的信息。
如果不指定  `-f` ，也可以通过该参数，指定 `hbase:meta`表的一个行键，格式类似于 `tableName,startKey,timestamp.regionName.`，然后会自动根据 `hbase:meta`表的行键 找到该 Region 下的所有 HFile（主要靠 tableName 和 regionName 进行查找）

```
scan 'hbase:meta',{FILTER => "(PrefixFilter ('dev')"}
```

> [66. Catalog Tables](http://hbase.apache.org/book.html#arch.catalog)

### -v,--verbose

该参数主要配合 `-r` 参数使用；如果单独使用，会打印出行的数量


## 程序调用 示例

``` java
public static void main(String[] args) throws Exception {

    List<String> params = Arrays.asList(
            "-r", "test_table,,1521207497217.9983d7464bcf4488a0b89a51711048b5.",
            "-v"
    );
    args = params.toArray(new String[params.size()]);

    Configuration conf = HBaseConfiguration.create();
    conf.setFloat(HConstants.HFILE_BLOCK_CACHE_SIZE_KEY, 0);
    conf.set(HConstants.HBASE_DIR, "hdfs://s01.hbase.kail.xyz:8020/hbase");

    int ret = ToolRunner.run(conf, new HFilePrettyPrinter(), args);
    System.exit(ret);
}
```


## Read More

- [HBase – 存储文件HFile结构解析](http://hbasefly.com/2016/03/25/hbase-hfile)
- [71.7.4. StoreFile (HFile)](http://hbase.apache.org/book.html#hfile)
