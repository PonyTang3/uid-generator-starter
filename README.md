# uid-generator-springboot-starter

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

#### 项目说明

UidGenerator是百度开源的基于[Snowflake](https://github.com/twitter/snowflake)算法的唯一ID生成器，可在分布式环境下生成单调、递增的ID。详情参见：

  **[uid-generator](https://github.com/baidu/uid-generator/blob/master/README.zh_cn.md)**  

从官网，或者网上的使用说明可见，将其集成到springboot项目中，还是有点小麻烦。此项目对UidGenerator进行了starter化的封装，只要一行注释便可将其集成到项目中。

#### 新增的特性

1、spring-boot-starter风格的开箱即用。

2、可为UidGenerator配置独立的数据源，和业务系统的主数据源分开。

3、支持使用ZooKeeper进行WORKER_NODE分配，借由Paxos一致性算法使系统具备更高的可用性。



**快速开始**

1、在数据库(mysql)创建表

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

2、引入uid-generator-starter

	<dependency>
		<groupId>com.github</groupId>
		<artifactId>uid-generator-starter</artifactId>
		<version>最新的版本号</version>
	</dependency>
3、启用UidGenerator

```
@Transactional
@EnableUidGenerator
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

4、获取ID

	@Resource
	private UidGenerator uidGenerator;
	
	@Test
	public void contextLoads()  {
		for(int i=0;i<100;i++) {
			System.out.println("uid:"+uidGenerator.getUID());
		}
	}


**使用独立的数据源**



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
    url: jdbc:mysql://192.168.1.666:3306/uid
    password: admin
    username: 123456
```

只需在uid数据中创建WORKER_NODE表

多个业务系统可共用uid数据库，以现分布式ID



**使用zookeeper**

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
    #url: jdbc:mysql://192.168.1.666:3306/uid
    #password: root
    #username: root
  zookeeper: 
    #zk连接地址，集群模式则用逗号分开，如： 192.168.1.333:2181,192.168.1.555:2182,192.168.1.66:2183
    addrs: 192.168.1.333:2181 
    #authentication: admin:123456 #digest类型的访问秘钥，如：user:password，默认为不使用秘钥
```

无需创建WORKER_NODE表

追求高可用推荐使用zookeeper集群模式

**示例**



#### 
