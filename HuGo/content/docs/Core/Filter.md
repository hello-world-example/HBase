# HBase 内置过滤器简介

HBase 的过滤器可以让我们**在查询中添加更多的限制条件 来减少查询得到的数据量**，从而帮助用户**提高处理表数据的效率**。
所有的**过滤器都在服务端生效**，使被过滤掉的数据不会被传送到客户端。


## Filter 的继承结构


- Filter 
    - FilterBase 
        - `CompareFilter` 
            - `RowFilter` 
            - `FamilyFilter` 
            - `QualifierFilter` 
            - `ValueFilter` 
            - DependentColumnFilter 
        - `SingleColumnValueFilter` 
            - SingleColumnValueExcludeFilter 
        - `PrefixFilter` 
        - `PageFilter` (*.fiter)
        - KeyOnlyFilter 
        - FirstKeyOnlyFilter 
            - FirstKeyValueMatchingQualifiersFilter 
        - InclusiveStopFilter 
        - `TimestampsFilter` 
        - ColumnCountGetFilter 
        - ColumnPaginationFilter 
        - ColumnPrefixFilter 
        - RandomRowFilter 
        - ColumnRangeFilter 
        - `MultipleColumnPrefixFilter` 
        - `MultiRowRangeFilter`
        - `FuzzyRowFilter`
        - SkipFilter 
        - WhileMatchFilter 
    - `FilterList` 
    - FilterWrapper 


## CompareFilter

CompareFilter **返回匹配的行**。


**构造函数**：

``` java
public CompareFilter(final CompareOp compareOp, final ByteArrayComparable comparator) {
    this.compareOp = compareOp;
    this.comparator = comparator;
}
```

**比较运算符**：

``` java
public enum CompareOp {
    /** 小于 */
    LESS,
    /** 小于等于 */
    LESS_OR_EQUAL,
    /** 等于 */
    EQUAL,
    /** 不等于 */
    NOT_EQUAL,
    /** 大于等于 */
    GREATER_OR_EQUAL,
    /** 大于 */
    GREATER,
    /** 不操作 */
    NO_OP
}
```
**比较器**：

- **BinaryComparator**    使用`Bytes.compareTo()`进行比较
- **BinaryPrefixComparator**    使用 `Bytes.compareTo()` 进行前缀匹配
- NullComparator    不做匹配，只判断当前值是否为 null
- LongComparator    `longValue.compareTo(Bytes.toLong(value))`

一下三种只能 使用 `EQUAL` 和 `NOT_EQUAL` 运算符

- BitComparator    使用 异或非 进行比较
- **RegexStringComparator**    正则匹配
- **SubstringComparator**    子串匹配


### RowFilter

筛选出 匹配的行键


### FamilyFilter

筛选出 匹配的列族


### QualifierFilter

筛选出 匹配的列限定符


### ValueFilter

筛选出 匹配的值


### DependentColumnFilter

> 指定一个参考列来过滤其他列的过滤器，过滤的原则是**基于参考列的时间戳来进行筛选**。

> [HBase--DependentColumnFilter（参考列过滤器 ）详解](https://www.cnblogs.com/MOBIN/p/5005871.html)
















## 专用过滤器


### SingleColumnValueFilter

与 `ValueFilter` 类似，但是可以**值针对某个列限定符**里面的值进行过滤。

#### SingleColumnValueExcludeFilter

与`SingleColumnValueFilter` 的不同之处在于，**不返回指定的列限定符 所有的列**


### PrefixFilter

匹配到的行键前缀的行会被返回。**当遇到比前缀大的行时，扫描结束**



### PageFilter

从起始行键扫描，返回指定的条数。
翻页的时候客户端需要记录上次返回的会有一个行键，下次作为起始行键。
起始行键是包含在返回结果中的，如果不想包含起始行键，在行键后拼接一个0字节的数组。
``` java
scan.setStopRow(Bytes.padTail(someBytes, 0));
// 或
scan.setStopRow(Bytes.add(someBytes, new byte[0]));
```




### KeyOnlyFilter

这个过滤器的功能就是**只返回每行的行键，值全部为空**，这对于只关注于行键的应用场景来说非常合适，这样忽略掉其值就可以减少传递到客户端的数据量，能起到一定的优化作用；



### FirstKeyOnlyFilter

只返回第一个列限定符所在的列


#### FirstKeyValueMatchingQualifiersFilter

通过设置一组需要匹配的列，只要匹配到任意一个列就会停止这一行的扫描操作进行下一行的扫描。




### InclusiveStopFilter

scan 的时候开始行被包含在内，但是结束行被排除在外，**使用该过滤器，会返回结束行**。


### TimestampsFilter

获取指定的时间戳集合
`scan.setTimeRange()` 可以指定时间戳范围


### ColumnCountGetFilter

限制 Get 操作返回的列数。
如果某一个行的列数超过限定的值，会停止扫描，所以**不适合 scan 使用**


### ColumnPaginationFilter

`PageFilter` 是基于行分页，该过滤器是基于列分页


### ColumnPrefixFilter

与`PrefixFilter`类似，该过滤器可以前缀匹配 列限定符 


### RandomRowFilter

根据设定的概率随机选择返回的行。
过滤器内部调用 Java 的随机方法产生一个随机数，如果产生的随机数小于设定的概率则包含改行，否则排除。
如果设定概率小于0，则全部排除，如果设置概率大于1，则全部包含


### ColumnRangeFilter

根据指定的列的范围进行筛选


### MultipleColumnPrefixFilter

过个列前缀过滤，构造方法是一个前缀数组

### MultiRowRangeFilter

多个行键过滤，构造方法是一个范围列表


### FuzzyRowFilter

`FuzzyRowFilter` 是对 **行键模糊匹配** 的优化版。是扫描更加快速。
它需要 一对儿信息（行键，模糊信息）以匹配行键。
模糊信息（fuzzy）是以`0`或`1`作为其值的字节数组：
- `0` - 相同位置上的 RowKey 的字节必须匹配（**不模糊匹配**）
- `1` - 这个位置上的 RowKey 的字节可以不同于提供的行密钥中的字节（**模糊匹配**）

示例: 假设行键的格式是 `userId_actionId_year_month`. 
`userId`的长度是`4`, `actionId`的长度是`2` ，`year` 和 `month`的长度分别是`4`和`2`
假设我们需要在**任意一年**的**一月**中获取执行**特定操作**（编码为`99`）的**所有用户**，我们可以这样。
pair(row key, fuzzy info) 信息如下: row key = `????_99_????_01` (用`?`代替任何字符) ，fuzzy info = `new byte[]{1,1,1,1,   0,   0,0,   0,   1,1,1,1,   0,   0,0}`

``` java
Pair<byte[], byte[]> pair = Pair.newPair(
    Bytes.toBytes("????_99_????_01"), 
    new byte[]{1,1,1,1,   0,   0,0,   0,   1,1,1,1,   0,   0,0}
);
scan.setFilter(new FuzzyRowFilter(Arrays.asList(pair)));
```

> [FuzzyRowFilter](http://hbase.apache.org/apidocs/org/apache/hadoop/hbase/filter/FuzzyRowFilter.html)




## 附加过滤器

### SkipFilter

该过滤器用于包装其它过滤器，匹配的过滤器，被该过滤器包装后会跳过。相当于 **不等于**


### WhileMatchFilter

该过滤器用于包装其它过滤器，返回从匹配开始，第一次遇到不匹配的之前的匹配到的数据。




## FilterList

多个过滤器可以组合使用，组合情况有一下两种

``` java
public static enum Operator {

    /** 所有过滤器都必须满足 */
    MUST_PASS_ALL,
    /** 只用满足其中一个就行 */
    MUST_PASS_ONE
}
```

### 只返回满足条件的行键信息

``` java
FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
filterList.addFilter(new PrefixFilter(Bytes.toBytes("dev")));
filterList.addFilter(new KeyOnlyFilter());
```




## Read More

- [68. Client Request Filters](http://hbase.apache.org/book.html#client.filter)
- [Filters In Hbase Shell](http://blog.csdn.net/u014034934/article/details/74330848)
- [HBase内置过滤器的一些总结](http://blog.csdn.net/cnweike/article/details/42920547)
