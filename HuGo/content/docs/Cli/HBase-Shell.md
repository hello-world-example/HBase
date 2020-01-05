
# hbase shell

## 基础命令

``` bash
# 进入
ᐅ ./bin/hbase shell
# 退出
hbase> exit


# 查看帮助
ᐅ ./bin/hbase shell -h
# 或
hbase> help
hbase> help 'group name'
hbase> help 'command'

# Debug 模式
ᐅ ./bin/hbase shell -d

# 启动或者关闭 debug 模式
hbase> debug
# 查看是否启动 debug 模式
hbase> debug?
```

## General ✔️

```bash
# status == status 'summary'
hbase> status
1 active master, 0 backup masters, 1 servers, 0 dead, 2.0000 average load
hbase> status 'simple'
hbase> status 'detailed'
hbase> status 'replication'

# 表操作示使用文档
hbase> table_help

# 
hbase> version
1.2.0-cdh5.7.0, rUnknown, Wed Mar 23 11:39:14 PDT 2016

# 
hbase> whoami
root (auth:SIMPLE)
    groups: root
```



## namespace ✔️

```bash
# 列出所有的命名空间
hbase> list_namespace
NAMESPACE                                                                                                                                                                                  
default                                                                                                                                                                                    
hbase 

# 列出名空间下的表 
hbase> list_namespace_tables 'hbase'
TABLE                                                                                                                                                                                      
meta                                                                                                                                                                                       
namespace

# 查看命名空间的描述（自定义信息）
hbase> describe_namespace 'hbase'
DESCRIPTION                                                                                                                                                                                
{NAME => 'hbase'}                                                                                                                                                                          

# 创建命名空间
hbase> create_namespace "kail-test"
# 携带自定义属性
hbase> create_namespace "kail02", {"author"=>"kail", "create_time"=>"2020-01-05 20:07:52"}

# 增加属性
hbase> alter_namespace 'kail02', {METHOD => 'set', 'email' => 'ykb553@163.com'}
# 删除属性
hbase> alter_namespace 'kail02', {METHOD => 'unset', NAME=>'email'}
# 【无法修改表名】该操作只是增加了一个 NAME 属性
hbase> alter_namespace 'kail02', {METHOD => 'set', NAME=>'kail'}
hbase> describe_namespace 'kail02'                                                                                                                                                                          
{NAME => 'kail02', NAME => 'kail', author => 'kail', create_time => '2020-01-05 20:07:52'}      

# 删除 namespace，没有表才能删除
hbase> drop_namespace 'kail02'
```



## ddl ✔️

### list 列出所有表

``` bash
hbase> list
# 表名支持正则匹配
hbase> list 'abc.*'
```

### create / drop

> 详见： [HBase 列族配置]({{< relref "/docs/Core/Family-Properties.md" >}})  

```bash
# 在 namespace kail 下 创建 t_test 表，列族是 F
hbase> create 'kail:t_test','F'
# 保留 5 个版本
hbase> create 't_test', {NAME => 'F1', VERSIONS => 5}

# 【pre-split】
# 默认创建的表只分配一个 region 达不到负载均衡的效果，如果能提前预测 region，可以在创建表的时候进行划分
hbase> create 't_test', 'F', SPLITS => ['10', '20', '30', '40']
hbase> create 't_test', 'F', {NUMREGIONS => 15, SPLITALGO => 'HexStringSplit'}

# 删除表
hbase> disable 'kail:t_test'
hbase> drop 'kail:t_test'
```

### desc / describe  查看表结构

> 详见： [HBase 列族配置]({{< relref "/docs/Core/Family-Properties.md" >}})  

```bash
hbase> describe 'kail:t_test'
Table kail:t_test is ENABLED                                                                                 
kail:t_test                                                                
COLUMN FAMILIES DESCRIPTION                                                                                 
{NAME => 'F', DATA_BLOCK_ENCODING => 'NONE', BLOOMFILTER => 'ROW', REPLICATION_SCOPE => '0', VERSIONS => '1', COMPRESSION => 'NONE', MIN_VERSIONS => '0', TTL => 'FOREVER', KEEP_DELETED_CELLS => 'FALSE', BLOCKSIZE => '65536', IN_MEMORY => 'false', BLOCKCACHE => 'true'}                                                                                                          
```

### get_table / exists / disable / enable / is_disabled / is_enabled

```bash
# 获取表的引用
hbase> t = get_table 'kail:t_test'
# 查看操作帮助
hbase> t.help
# 计数
hbase> t.count
# 扫描
hbase> t.scan

# 表是否存在
hbase> exists 'kail:t_test'
# 是否禁用
hbase> is_disabled 'kail:t_test'
# 是否启用
hbase> is_enabled 'kail:t_test'

# 修改表之前需要先禁用表
hbase> disable 'kail:t_test'
# alter 之后，重新启用
hbase> enable 'kail:t_test'
```

### alter / alter_async / alter_status

```bash
# 修改列族属性（版本号）
hbase> alter 'kail:t_test', NAME => 'F', VERSIONS => 5
hbase> alter 'kail:t_test', {NAME => 'F', VERSIONS => 15}

# 增加列族
alter 'kail:t_test', 'F2'

# 删除列族
alter 'kail:t_test', 'delete' => 'F2'

# 设置只读
alter 'kail:t_test', READONLY
# 取消只读模式（删除列族）
alter 'kail:t_test', 'delete' => 'READONLY'

```



### locate_region 查看 rowkey 所在的 region

```bash
hbase> locate_region 'kail:t_test', '1'
HOST                        REGION                                                                                                                                     
quickstart.cloudera:60020   {ENCODED => 6b03d15bc0abe09c86845318e8056a16, NAME => 'kail:t_test,,1578227112676.6b03d15bc0abe09c86845318e8056a16.', STARTKEY => '', ENDKEY => ''}             
```



### show_filters 查看支持的 Filter

> 详见： [HBase 内置过滤器简介]({{< relref "/docs/Core/Filter.md" >}})

```bash
hbase> show_filters
ColumnPrefixFilter                                                                                 
TimestampsFilter                                                                                 
PageFilter                                                                                 
MultipleColumnPrefixFilter                                                                                 
FamilyFilter                                                                                 
ColumnPaginationFilter
...
```

## dml ✔️

### put / append

```bash
# put 表、rowkey、列:列限定符、值
hbase> put 'kail:t_test', 'r1', 'F:c1', 'value1'
# put 表、rowkey、列族:列限定符、值、【版本】
hbase> put 'kail:t_test', 'r2', 'F:c1', 'value2', 1

# 给 value 追加内容（-new）
hbase> append 'kail:t_test', 'r1', 'F:c1', '-new'
hbase> append 'kail:t_test', 'r1', 'F:c1', '-new', ATTRIBUTES=>{'kkk'=>'vvv'}
```

### count

```bash
# 统计表的数据量，默认 INTERVAL => 1000, CACHE => 10
hbase> count 'kail:t_test'
hbase> count 'kail:t_test', INTERVAL => 5000
```

### scan
``` bash
# 遍历最新版本的表数据（）
hbase> scan 'kail:t_test'
hbase> scan 'kail:t_test', {LIMIT => 10, VERSIONS => 10}
# 只获取 F列、从 r2 行开始，获取 10条，每条数据获取最近 10个版本的数据
hbase> scan 'kail:t_test', {COLUMNS => ['F'], STARTROW => 'r2', LIMIT => 10, VERSIONS => 10}

hbase> scan 'kail:t_test', {COLUMNS => ['F:c1'], LIMIT => 10}
hbase> scan 'kail:t_test', {COLUMNS => ['F:c1:toInt']}
hbase> scan 'kail:t_test', {COLUMNS => ['F:c1:c(org.apache.hadoop.hbase.util.Bytes).toInt']}
```

### get

``` bash
# 获取 r1 的所有列
hbase> get 'kail:t_test', 'r1'
# 获取 r1 的 F列
hbase> get 'kail:t_test', 'r1', 'F'
# 获取 r1 的 F列
hbase> get 'kail:t_test', 'r1', {COLUMN => ['F'] }
hbase> get 'kail:t_test', 'r1', {COLUMN => 'F', TIMESTAMP => 1578235640022, VERSIONS => 4}
# 指定时间范围的数据
hbase> get 'kail:t_test', 'r1', {TIMERANGE => [ts1, ts2]}
# 值过滤器
hbase> get 'kail:t_test', 'r1', {FILTER => "ValueFilter(=, 'binary:abc')"}
```
### incr / get_counter

``` bash
# 自增列
hbase> incr 'kail:t_incr','r1', 'F:i'
COUNTER VALUE = 3
hbase> incr 'kail:t_incr','r2', 'F:i'
COUNTER VALUE = 1

# 查看表数据
hbase> scan 'kail:t_incr', {VERSIONS => 100}
ROW   COLUMN+CELL                                                                                                                                
 r1   column=F:i, timestamp=1578236311698, value=\x00\x00\x00\x00\x00\x00\x00\x03                                       
 r2   column=F:i, timestamp=1578236315391, value=\x00\x00\x00\x00\x00\x00\x00\x01

# 获取自增列值
hbase> get_counter 'kail:t_incr','r1', 'F:i'
COUNTER VALUE = 3

# 加指定的值
hbase> incr 'kail:t_incr','r2', 'F:i', 10
COUNTER VALUE = 11
```
### get_splits 获取分区列表

```bash
hbase> get_splits 'kail:t_test'
Total number of splits = 1

=> []
```

### delete / deleteall / truncate / truncate_preserve

```bash
# 语法错误
hbase> delete 'kail:t_test','r2'
# delete 需要精确到时间戳
hbase> delete 'kail:t_test','r2', 'F:c1', 1
hbase> delete 'kail:t_test','r2', 'F:i', 1578236267165

# deleteall 精确到行即可
hbase> deleteall 'kail:t_test','r1'

# 清除数据，并且清除了分区
hbase> truncate 'kail:t_test'
Truncating 'kail:t_test' table (it may take a while):
 - Disabling table...
 - Truncating table...
hbase> is_enabled 'kail:t_test'
true

# 清除数据，不清分区
hbase> truncate_preserve 'kail:t_test'
```



## tools

assign, balance_switch, balancer, balancer_enabled, catalogjanitor_enabled, catalogjanitor_run, catalogjanitor_switch, close_region, compact, compact_mob, compact_rs, flush, major_compact, major_compact_mob, merge_region, move, normalize, normalizer_enabled, normalizer_switch, split, trace, unassign, wal_roll, zk_dump



## replication

add_peer, append_peer_tableCFs, disable_peer, disable_table_replication, enable_peer, enable_table_replication, list_peers, list_replicated_tables, remove_peer, remove_peer_tableCFs, set_peer_tableCFs, show_peer_tableCFs



## snapshots

clone_snapshot, delete_all_snapshot, delete_snapshot, list_snapshots, restore_snapshot, snapshot



## configuration

update_all_config, update_config



## quotas

list_quotas, set_quota



## security

grant, list_security_capabilities, revoke, user_permission



## procedures

abort_procedure, list_procedures



## visibility labels

add_labels, clear_auths, get_auths, list_labels, set_auths, set_visibility