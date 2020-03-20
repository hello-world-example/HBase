# Delete 操作语义

单独把 `Delete` 拎出来，是因为 `Delete` 的 **实际效果** 与 **语义** 可能不符，比如常会出现

- 本来想删除一个版本的数据，结果删除了一批
- 删除之后重新写入，虽然操作成功，但是读取不到数据
- API 比较类似，很容易搞混，想象中与实际操作区别很大
- ...



## 简介

HBase 支持多版本，即支持向同一个 `rowkey:family:qualifier` 中写入多个 `timestamp` 不同的 `value`。`timestamp` 不仅可以由服务器根据写入时间生成，也可以由用户指定。尤其是在由用户指定的时候需要更加注意，因为常常会出现**后写入数据的 `timestamp` 比之前写入的数据的  `timestamp`  还小的情况**，即写入一个很久之前数据的情况。

因为存储采用 `LSM (Log-Structured Merge Tree)` 结构， HBase 的删除操作并**不会立即将数据从磁盘上删除**，当执行删除操作时，会新插入一条相同的 KeyValue 数据，通过 keytype（如： `keytype=Delete`）标记删除类型。这意味着数据删除实际上也是一个普通的 Put 追加操作，直到发生 `major_compact` 操作时，数据才会被真正的从磁盘上删除，删除标记也会从 `StoreFile` 删除。



## hbase shell

### 数据准备

```bash
# 创建一个表，最多可以有 5个 版本
> create 'test', {NAME => 'f', VERSIONS => 5}

# 插入三条数据，版本分别是 1/2/3
> put 'test', 'r1', 'f:c1', '1', 1
> put 'test', 'r1', 'f:c1', '2', 2
> put 'test', 'r1', 'f:c1', '3', 3

# 查看数据
> scan 'test'
ROW    COLUMN+CELL                                            
 r1    column=f:c1, timestamp=3, value=3                 

# 查看所有版本
> scan 'test', { VERSIONS => 5 }
ROW    COLUMN+CELL                                            
 r1    column=f:c1, timestamp=3, value=3                                                                                                          
 r1    column=f:c1, timestamp=2, value=2                                                                                                          
 r1    column=f:c1, timestamp=1, value=1        
```

### delete

- `delete '表名', '行键', '列'`
- `delete '表名', '行键', '列', 时间戳`

#### 删除列（的最新版本）

> `delete '表名', '行键', '列'`

```bash
# 删除 r1 的 c1 的 ❤❤❤ 最新版本的数据 ❤❤❤ 
> delete 'test', 'r1', 'f:c1'

> scan 'test'
ROW    COLUMN+CELL                                                                                                                                
 r1    column=f:c1, timestamp=2, value=2                                    
 
> scan 'test', { VERSIONS => 5 }
ROW    COLUMN+CELL                                                                                                                                
 r1    column=f:c1, timestamp=2, value=2                                                                                                          
 r1    column=f:c1, timestamp=1, value=1                     

# 查看删除标示
> scan 'test', { VERSIONS => 5, RAW => true}
ROW    COLUMN+CELL                                                                                                                                
 r1    column=f:c1, timestamp=3, type=Delete                                                                                                      
 r1    column=f:c1, timestamp=3, value=3                                                                                                          
 r1    column=f:c1, timestamp=2, value=2                                                                                                          
 r1    column=f:c1, timestamp=1, value=1  
 
# ================================================================================
 
# ❤❤❤ put 之后仍然”查不到“ ❤❤❤ 
> put 'test', 'r1', 'f:c1', '3', 3
> scan 'test', { VERSIONS => 5 }
ROW    COLUMN+CELL                                            
 r1    column=f:c1, timestamp=2, value=2                                            
 r1    column=f:c1, timestamp=1, value=1
 
# ================================================================================

# ❤❤❤ major_compact 之后还是查不到 ❤❤❤ 
# ❤❤❤ major_compact 只是合并合并 HFile 文件 ❤❤❤ 
# ❤❤❤ 但是现在数据还在 MemeStore 中，需要先 flush 生成 HFile，再 major_compact  ❤❤❤ 
> major_compact 'test'
> put 'test', 'r1', 'f:c1', '3', 3
> scan 'test', { VERSIONS => 5 }
ROW    COLUMN+CELL                                            
 r1    column=f:c1, timestamp=2, value=2                                            
 r1    column=f:c1, timestamp=1, value=1

> scan 'test', { VERSIONS => 5, RAW => true}
ROW    COLUMN+CELL                                            
 r1    column=f:c1, timestamp=3, type=Delete                                            
 r1    column=f:c1, timestamp=3, value=3
 r1    column=f:c1, timestamp=2, value=2                                            
 r1    column=f:c1, timestamp=1, value=1
 
# ================================================================================

> flush 'test'
# ❤❤❤ flush 之后 被标记数据已经删除，但是删除标记还在
# ❤❤❤ 这个时候 put 数据还是看不到的 ❤❤❤ 
> scan 'test', { VERSIONS => 5, RAW => true}
ROW    COLUMN+CELL                                            
 r1    column=f:c1, timestamp=3, type=Delete  
# flush 之后 put 再 flush，这时候会发现这条还能查到，因为每次 flush 会生成一个新的 HFile
# 新插入的数据 和 上次 flush 到 HFile 中的 删除标记，不在一个 HFile 里面
# 如果 删除标记和被删除的数据同时刷到一个 HFile 中，则 scan RAW 的时候就是看不到的
#r1    column=f:c1, timestamp=3, value=3
 r1    column=f:c1, timestamp=2, value=2                                            
 r1    column=f:c1, timestamp=1, value=1

> major_compact 'test'
# 删除标记没有了
> scan 'test', { VERSIONS => 5, RAW => true}
ROW    COLUMN+CELL                                            
 r1    column=f:c1, timestamp=2, value=2                                            
 r1    column=f:c1, timestamp=1, value=1
 
# 此时再 Put 就可以看到了，回到最初的状态
> put 'test', 'r1', 'f:c1', '3', 3
```

#### 删除指定的版本

> `delete '表名', '行键', '列', 时间戳`

```bash
> delete 'test', 'r1', 'f:c1', 2

# 查不到 版本 2
> scan 'test', { VERSIONS => 5 }
ROW    COLUMN+CELL                                           
 r1    column=f:c1, timestamp=3, value=3                                           
 r1    column=f:c1, timestamp=1, value=1

# Delete 删除标记
> scan 'test', { VERSIONS => 5, RAW => true}
ROW    COLUMN+CELL                                                                                                                                
 r1    column=f:c1, timestamp=3, value=3                                                                                                          
 r1    column=f:c1, timestamp=2, type=Delete                                                                                                      
 r1    column=f:c1, timestamp=2, value=2                                                                                                          
 r1    column=f:c1, timestamp=1, value=1
 
 # 后续操作与删除列效果一致，不再赘述
```



### deleteall

- `deleteall '表名', '行键'`
- `deleteall '表名', '行键', '列'`
- `deleteall '表名', '行键', '列', 时间戳`

#### 删除行

> `deleteall '表名', '行键'`

```bash
# 删除一行
> deleteall 'test', 'r1'

# 查不到数据
> scan 'test'                                         
0 row(s)

# ❤❤❤ 注意删除标记 ❤❤❤
> scan 'test', { VERSIONS => 5, RAW => true}
ROW    COLUMN+CELL  
 r1    column=f:, timestamp=1584620555935, type=DeleteFamily  
 r1    column=f:c1, timestamp=3, value=3
 r1    column=f:c1, timestamp=2, value=2
 r1    column=f:c1, timestamp=1, value=1

# ================================================================================

# ❤❤❤ 对 r1 的 所有小于 timestamp=1584620555935 的新的插入操作，都是不可见的
> put 'test', 'r1', 'f:c1', '3', 3
> put 'test', 'r1', 'f:c2', '3', 3

# ❤❤❤ 大于 timestamp=158462055593 的数据可见
> put 'test', 'r1', 'f:c1', '1584620555935+1', 1584620555936

# ================================================================================

# 多次删除操作总是排在前面
> scan 'test', { VERSIONS => 5, RAW => true}
ROW    COLUMN+CELL                                                                                                                                
 r1    column=f:, timestamp=1584622644980, type=DeleteFamily                                                                                      
 r1    column=f:, timestamp=1584620555935, type=DeleteFamily                                                                                      
 r1    column=f:c1, timestamp=1584620555936, value=1584620555935+1                                                                                
 r1    column=f:c1, timestamp=3, value=3                                                                                                          
 r1    column=f:c1, timestamp=2, value=2                                                                                                          
 r1    column=f:c1, timestamp=1, value=1   
```

#### 删除列

> `deleteall '表名', '行键', '列'`

```bash
deleteall 'test', 'r1', 'f:c1'

# ❤❤❤ 注意删除标记 ❤❤❤
> scan 'test', { VERSIONS => 5, RAW => true}
ROW    COLUMN+CELL                                                                                                                                
 r1    column=f:c1, timestamp=1584623119728, type=DeleteColumn                                                                                    
 r1    column=f:c1, timestamp=3, value=3                                                                                                          
 r1    column=f:c1, timestamp=2, value=2                                                                                                          
 r1    column=f:c1, timestamp=1, value=1  
 
# ================================================================================
# ❤❤❤ 区别 ❤❤❤
# × 不可见
> put 'test', 'r1', 'f:c1', '3', 3
# √ 可见
> put 'test', 'r1', 'f:c2', '3', 3
```



#### 删除时间戳

> `deleteall '表名', '行键', '列', 时间戳`

```bash
# 删除版本 2 及其之前的数据
> deleteall 'test', 'r1', 'f:c1', 2

# 版本2及其之前的版本都不可见
> scan 'test', { VERSIONS => 5 }
ROW    COLUMN+CELL                                           
 r1    column=f:c1, timestamp=3, value=3
 
> scan 'test', { VERSIONS => 5 ,RAW => true}
ROW    COLUMN+CELL                                                                                                                                
 r1    column=f:c1, timestamp=3, value=3                                                                                                          
 r1    column=f:c1, timestamp=2, type=DeleteColumn                                            
 r1    column=f:c1, timestamp=2, value=2                                                                                                          
 r1    column=f:c1, timestamp=1, value=1     
```



### delete / deleteall 小结

> - delete： 删除指定版本
>   - Delete
>   - DeleteFamilyVersion
>
> - deleteall: 删除指定版本**之前的所有范围数据**
>   - DeleteColumn
>   - DeleteFamily

|              操作               |     delete     |               deleteall                |
| :-----------------------------: | :------------: | :------------------------------------: |
|           仅限定行键            |   ~~不支持~~   |         当前时间之前的所有数据         |
|       行键 +  列族/限定符       | 最新时间的数据 | **指定列** **最新时间** 之前的所有数据 |
| 行键 +  列族/限定符 + timestamp | 指定时间的数据 | **指定列** **指定时间** 之前的所有数据 |



## Client API

### ❤ 对比 ❤

- `t`  TableName
- `r`  RowKey
- `f`  Family / ColumnFamily
- `c`  Column / Qualifier
- `ts` timestamp
- `tp` org.apache.hadoop.hbase.KeyValue.Type

| 范围     |            tp             | Client API                | Shell                        |
| -------- | :-----------------------: | ------------------------- | ---------------------------- |
| 之前     |      `DeleteFamily`       | `new Delete(rk)`          | `deleteall 't','r'`          |
| 之前     |      `DeleteFamily`       | `addFamily(f)`            | `deleteall 't','r','f'`      |
| 之前     |      `DeleteFamily`       | `addFamily(f, ts)`        | `deleteall 't','r','f',ts`   |
| **当前** | **`DeleteFamilyVersion`** | `addFamilyVersion(f, ts)` | 无                           |
| **当前** |       **`Delete`**        | `addColumn(f,c)`          | `delete 't','r','f:c'`       |
| 之前     |      `DeleteColumn`       | `addColumns(f,c)`         | `deleteall 't','r','f:c'`    |
| **当前** |       **`Delete`**        | `addColumn(f,c,ts)`       | `delete 't','r', 'f:c',ts`   |
| 之前     |      `DeleteColumn`       | `addColumns(f,c,ts)`      | `deleteall 't','r','f:c',ts` |

### 其他操作

| Delete 实例接口    | 描述                                          |
| ------------------ | --------------------------------------------- |
| `add(cell)`        | 通过 `KeyValue(r,f,c,ts,tp)` 自定义类型       |
| `setTimestamp(ts)` | `addXxx` 接口的 ts 参数，默认取最新的数据版本 |
| `setTTL(ttl)`      | 抛出 `UnsupportedOperationException` 异常     |

## 小结

- 在表结构设计的开始阶段就要考虑 Delete 操作带来的问题
- 如果写入数据不可见，可在 scan 时启用 RAW 来查看删除标示 `scan 't',{ .. ,RAW => true}`
-  `flush` 和 `compact` 对 HBase 的性能影响很大，设计到大量的 IO 操作，一般由 HBase 自己控制，慎用
- `major_compact` 并不一定能消除删除标记，因为标记可能还没有生成 HFile，需要先 `flush` `MemStore`



## Read More

- [HBase多版本语义与delete语义的历史遗留问题](https://mp.weixin.qq.com/s/HkkdTzBdKOJSc2odLaJlww)
- [HBase删除数据的原理](https://blog.csdn.net/cenjianteng/article/details/96645447)
- JavaDoc: [org.apache.hadoop.hbase.client.Delete](https://hbase.apache.org/apidocs/org/apache/hadoop/hbase/client/Delete.html) 