# uid-generator-starter

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

#### 项目说明

UidGenerator是百度开源的基于Snowflake算法的唯一ID生成器，使用java语言实现，可在分布式环境下生成单调递增的ID。详情参见：

  **[uid-generator](https://github.com/baidu/uid-generator/blob/master/README.zh_cn.md)**  

从官网说明或者网上的使用教程可见，将其集成到springboot项目中，还是有点小麻烦的。此项目对uid-generator进行了springboot Starter风格的封装，只要一行注释便可将其集成到项目中，同时还增加一些实用的特性。



#### 新增的特性

1、spring-boot-starter风格的开箱即用。

2、可为uid-generator独立设置数据源，和业务系统的主数据源分开。

3、支持使用ZooKeeper进行WORKER ID分配，藉由ZK的Paxos强一致性算法获取更高的可用性。

**快速开始**

1、引入uid-generator-starter

	<dependency>
		<groupId>com.github</groupId>
		<artifactId>uid-generator-starter</artifactId>
		<version>最新的版本号</version>
	</dependency>



2、在数据库(mysql)中创建WORKER_NODE表

```
DROP TABLE IF EXISTS WORKER_NODE;
CREATE TABLE WORKER_NODE
(
	ID BIGINT NOT NULL AUTO_INCREMENT COMMENT 'auto increment id',
	HOST_NAME VARCHAR(64) NOT NULL COMMENT 'host name',
	PORT VARCHAR(64) NOT NULL COMMENT 'port',
	TYPE INT NOT NULL COMMENT 'node type: ACTUAL or CONTAINER',
	LAUNCH_DATE DATE NOT NULL COMMENT 'launch date',
	MODIFIED TIMESTAMP NOT NULL COMMENT 'modified time',
	CREATED TIMESTAMP NOT NULL COMMENT 'created time',
	PRIMARY KEY(ID)
)
COMMENT='DB WorkerID Assigner for UID Generator',ENGINE = INNODB;
```

3、注解启用uid-generator

```
@Transactional
@EnableUidGenerator //启用uid-generator
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

4、使用

	@Resource
	private UidGenerator uidGenerator;
	
	@Test
	public void contextLoads()  {
		for(int i=0;i<100;i++) {
			System.out.println("uid:"+uidGenerator.getUID());
		}
	}



**使用独立的数据源**

在数据库（uid-db）中创建WORKER_NODE表，使用其作为uid-generator的专用数据库

多个业务系统只需将uid-generator的数据库设置为uid-db即可

```
#---------------------- 业务配置   -----------------------
spring:
  datasource: #业务数据源
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/yewu1
    password: admin
    username: 123456
#---------------------- uid-generator   -----------------------
uid-generator: 
  #time-bits: 28 #可选配置, 如未指定将采用默认值
  #worker-bits: 22 #可选配置, 如未指定将采用默认值
  #seq-bits: 13 #可选配置, 如未指定将采用默认值
  #epoch-str: 2016-05-20 #可选配置, 如未指定将采用默认值
  #boost-power: 3 #可选配置, 如未指定将采用默认值
  #padding-factor: 50 #可选配置, 如未指定将采用默认值
  #schedule-interval:  #可选配置, 如未指定则不启用此功能
  datasource: #使用独立的数据源,如未指定将采用应用系统的数据源
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://192.168.1.666:3306/uid-db
    password: admin
    username: 123456
```



**使用zookeeper**



追求高可用推荐使用zookeeper集群模式

```
#---------------------- 业务配置   -----------------------
spring:
  datasource: #业务数据源
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/yewu?
    password: admin
    username: 123456
#---------------------- uid-generator   -----------------------
uid-generator: 
  #time-bits: 28 #可选配置, 如未指定将采用默认值
  #worker-bits: 22 #可选配置, 如未指定将采用默认值
  #seq-bits: 13 #可选配置, 如未指定将采用默认值
  #epoch-str: 2016-05-20 #可选配置, 如未指定将采用默认值
  #boost-power: 3 #可选配置, 如未指定将采用默认值
  #padding-factor: 50 #可选配置, 如未指定将采用默认值
  #schedule-interval:  #可选配置, 如未指定则不启用此功能
  #datasource: #使用独立的数据源,如未指定将采用应用系统的数据源
    #driver-class-name: com.mysql.cj.jdbc.Driver
    #url: jdbc:mysql://192.168.1.666:3306/uid-db
    #password: root
    #username: root
  zookeeper: 
    #zk连接地址，集群模式则用逗号分开，如： 192.168.1.333:2181,192.168.1.555:2182,192.168.1.66:2183
    addrs: 192.168.1.333:2181 
    #authentication: admin:123456 #digest类型的访问秘钥，如：user:password，默认为不使用秘钥
```



