
# Rowkey 设计

- Rowkey 是一段二进制码流，**最大长度为64KB**
- Rowkey 以**字典顺序排序**，其方法是，按照字母顺序，或者数字小大顺序，由小到大的形成序列
  - Rowkey：123,321,2 
  - 顺序为:     123,2,321
- 查找数据的时候，先查找 Rowkey 所在的 Region，然后将求路由到该 Region 获取数据
- 按单个 Rowkey 检索的效率是很高的，耗时在1毫秒以下

## 设计原则

### 避免热点

避免热点首先要了解 **Rowkey的排序规则** 和 **分区分类原理**

- RowKey 按照 **字典顺序** **从小到大**，如：  `1, 12,  27,  3,  4, 66`
- 如果没有预分区的话，刚开始所有的数据落在一个分区，当分区达到一定的阀值开始分裂，以上数据可能会分裂成   `1, 12,  27` 和 `3,  4, 66` 两个分区，每个分区记录 StartKey 和 EndKey，
- 多个分区会落在不同的机器上，避免热点的目的是为了**利用集群的能力** 和 **减少分裂的次数**，而**不是操作都落在一台机器上**



可以**让头几位尽量是随机的而不是规律的产生**，从而避免热点，大概有一下几种场景

- 自增序列、时间戳 等自增序列
  - 产生的数据类似于 `11,12,13,14,15,16` ，假如现在有两个分区
    -  `[0,14..)`:  `11,12,13` 
    -  `[14..,无穷)`:`14,15,16` 
    - 新生成 `19,20,21,22` 都会落在第二个分区
  - 对于自增序列，可以通过转置的方式
    - 比如对以上序列转置后变为，`11,21,31,41,51,61`
    - `[0,4..)`:  `11,12,13` 
    - `[4..,无穷)` : `14,15,16` 
    - 新生成 `19,20,21,22` -> `91,02,12,22` 会分别落在，两个分区
- 在头部加 Hash 值，产生随机性
  - 比如同样的自增序列，`a,b,c,d,e`，通过 `rowkey = hash(key) / num + key` 的方式生成，则可提高随机性

### 唯一性

- HBase 通过 `rowkey:列族:列限定符:version` 四个属性唯一标示一条数据
- 如果 列族设置的只保存一个 `version`，则相同的 `rowkey:列族:列限定符` 会互相覆盖
- `version` 可以通过程序自动生成，也可使用 时间戳，默认是 `Long.MAX_VALUE`，注意使用时间戳的时候，同一毫秒产生的多条数据会互相覆盖（`rowkey:列族:列限定符` 一致的情况下）



## 限制与技巧

- 因为 RowKey 前缀必须有一定的随机性，所以想通过 表级别的遍历 获取 最近新增数据是不可能的，除非 RowKey 是随时间顺序排列，这样又会导致热点
- RowKey 的随机性必须是根据现有数据可计算的，毕竟写入的数据是要读取的，假如随机生成 RowKey，以后要查找这条数据，但是无法二次生成该数据的 RowKey 或者 两次生成的结果不一样，就无法查找
- 所以随机性要根据现有数据生成，顺序查找也只能在 RowKey 的级别进行 顺序查找， 比如 获取最近的数据可以通过构建 `转置(id) + (Long.MAX_VALUE - 时间戳)` 形式的 RowKey，获取 `id=123` 的最近一条数据，只需要遍历 `321` 开头的第一个条数据即可
- 不要被 **RowKey 和 关系型表的设计** 限制住思维，**要知道  `rowkey:列族:列限定符:version` 四个属性唯一标示一条数据**，不仅 RowKey 有顺序，`列族:列限定符:version` 都是按照顺序保存的，**同一个 列族 无数个 列限定符** 都是可以的，列限定符是可以在运行时自动增加，所以同样需要参与设计
- 有人也称 HBase 为 KeyValue 键值对数据库，可以通过以下数据理解这种叫法

```
ROW                           COLUMN+CELL                                                                                                                                
03,9223370455878258,17870     column=C:AGE,    timestamp=1580976517000, value=222                                                                                           
03,9223370455878258,17870     column=C:ID,     timestamp=1580976517000, value=30                                                                                             
03,9223370455878258,17870     column=C:OTHER,  timestamp=1580976517000, value=222                                                                                         
03,9223370455878258,17870     column=C:UNAME,  timestamp=1580976517000, value=AAA3                                                                                        
03,9223370455878258,17870     column=C:_pk_,   timestamp=1580976517000, value=ID                                                                                           
03,9223370455878258,17870     column=C:_ts_,   timestamp=1580976517000, value=1580976517000                                                                                
03,9223370455878258,17870     column=C:_type_, timestamp=1580976517000, value=UPDATE 
```

## OpenTSDB 的 RowKey 设计

先通过 [Prometheus](https://prometheus.io/docs/concepts/data_model/) 的一条数据 了解监控数据的格式

```bash
<metric name>{<label name>=<label value>, ...} value

<监控指标名称>{<标签名1>=<标签值1>, <标签名2>=<标签值3>, ...} 监控数据

# /messages 的 POST 请求有 100 个，method/handler 等 标签 用来过滤
api_http_requests_total{method="POST", handler="/messages"} 100
```

OpenTSDB 可以通过 **指标名称和时间范围，并且根据给定的标签名称和标签的值作为过滤条件，以此查询符合条件的数据**

> 官方文档： [OpenTSDB 2.3 documentation](http://opentsdb.net/docs/build/html/index.html) » [User Guide](http://opentsdb.net/docs/build/html/user_guide/index.html) » [Storage](http://opentsdb.net/docs/build/html/user_guide/backends/index.html) » [Data Table Schema](http://opentsdb.net/docs/build/html/user_guide/backends/hbase.html#data-table-schema)

```bash
# RowKey
00000150E22700000001000001000002000004
'----''------''----''----''----''----'
metric  time   tagk1 tagv1 tagk2 tagv2

# Column Qualifiers 连限定符
将 Rowkey 中时间戳由原来的秒级别或毫秒级别统一转换成小时级别的
多余的秒数据或者毫秒数据作为 Column Qualifiers
```

## Read More

- [HBase Rowkey的散列与预分区设计](http://www.cnblogs.com/bdifn/p/3801737.html)
- [OpenTSDB 底层 HBase 的 Rowkey 是如何设计的](https://blog.csdn.net/b6ecl1k7BS8O/article/details/84207777)