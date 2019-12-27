
# hbase shell

## 基础

### 启动 退出

``` bash
./bin/hbase shell
```

``` bash
hbase(main):001:0> exit
```

### 帮助

命令行帮助
``` bash
./bin/hbase shell -h
```

shell 命令帮助
``` bash
hbase(main):001:0> help
```

### 启用 debug

``` bash
./bin/hbase shell -d
```

``` bash
# 启动或者关闭 debug 模式
hbase(main):001:0> debug

# 查看是否启动 debug 模式
hbase(main):001:0> debug?
```


## 命令

### ddl

#### list 列出所有表

表名支持 正则匹配
``` bash
hbase> list
hbase> list 'abc.*'
```


#### create 创建表




#### describe 查看表结构

#### alter 修改表结构





#### exists 检查表是否存在
#### disable 禁用表
#### enable 启用表
#### drop 删除表
#### is_disabled 查看是否禁用表
#### is_enabled 查看是否启用表






## dml

#### scan
``` bash



```

append, count, delete, deleteall, get, get_counter, incr, put, truncate, truncate_preserve

``` bash

```
``` bash

```
``` bash

```
``` bash

```
