
# 杂项

## 《HBase权威指南》 读书笔记

### 03 客户端API-基础支持

第三种介绍了使用 **Java API 对 HBase 进行 CRUD 操作**

- 创建 `HTable` 代价非常高（需要扫描`.META.`表）,建议在应用启动的时候创建，如果要使用多个，请使用 `HTablePool`
- 为每个线程创建各自的 `HTable` 实例
- 所有修改操作只保证 **行级别的原子性**

- [随书代码](https://github.com/larsgeorge/hbase-book)

### 06 可用客户端

- 在 hbase shell 中使用命令时，表名、列名、查看命令帮助的命令名(help "get") 必须用单引号或者双引号进行包裹

- Master服务器 的 WebUI 默认端口是 **60010** （`hbase.master.info.port`）
- Region服务器 的 WebUI 默认端口是 **60030** （`hbase.regionserver.info.port`）


### 08 架构

- 先联系Zookeeper 查找行键？


### 09 高级用法

- KeyValue 存储时**先按照行键排序**，当列族有多个单元格时**再按照列键排序**，单元格的多个版本**按照时间倒叙排序**
- 尽量将要查询的信息包含在行键中，因为行键的筛选效率最高
- HBase 适合高表，不适合宽表
- 







## 《HBase企业应用开发实战》 读书笔记

### 04 HBase 表结构设计

- 表、RowKey、列族 唯一确定一行数据
- 单元格（Cell） 所在的行是有序的
- RowKey是 不可分割的字节
- HBase 只能在 Rowkey 上建立索引






