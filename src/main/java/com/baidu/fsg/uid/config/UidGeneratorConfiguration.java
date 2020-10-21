package com.baidu.fsg.uid.config;

import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import com.baidu.fsg.uid.impl.CachedUidGenerator;
import com.baidu.fsg.uid.worker.DisposableWorkerIdAssigner;
import com.baidu.fsg.uid.worker.dao.WorkerNodeDAO;
import com.baidu.fsg.uid.worker.dao.WorkerNodeResposity;
import com.baidu.fsg.uid.worker.dao.WorkerNodeZK;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 配置
 * @author wangjie (https://github.com/wj596)
 *
 */
public class UidGeneratorConfiguration {

	private static final String COUNTER_ZNODE = "/uid-generator/worker_node";
	
	@Value("${uid-generator.time-bits:0}")
	private int timeBits;
	@Value("${uid-generator.worker-bits:0}")
	private int WorkerBits;
	@Value("${uid-generator.seq-bits:0}")
	private int seqBits;
	@Value("${uid-generator.epoch-str:}")
	private String epochStr;
	@Value("${uid-generator.boost-power:0}")
	private int boostPower;
	@Value("${uid-generator.padding-factor:0}")
	private int paddingFactor;
	@Value("${uid-generator.schedule-interval:0}")
	private int scheduleInterval;

	@Value("${uid-generator.datasource.driver-class-name:}")
	private String driverClassName;
	@Value("${uid-generator.datasource.url:}")
	private String url;
	@Value("${uid-generator.datasource.username:}")
	private String username;
	@Value("${uid-generator.datasource.password:}")
	private String password;

	@Value("${uid-generator.zookeeper.addrs:}")
	private String addrs;
	@Value("${uid-generator.zookeeper.authentication:}")
	private String authentication;
	@Value("${uid-generator.zookeeper.session-timeout-ms:15000}")
	private int sessionTimeoutMs;
	
	@Bean
	public WorkerNodeResposity workerNodeResposity(ObjectProvider<DataSource> dataSourcePvd) {
		if (StringUtils.isNotBlank(this.addrs)) {
			return this.insWorkerNodeZK();
		}

		DataSource dataSource = null;
		if (StringUtils.isNotBlank(this.driverClassName)&&
				StringUtils.isNotBlank(this.url)&& 
				StringUtils.isNotBlank(this.username)&& 
				StringUtils.isNotBlank(this.password)) {

			HikariDataSource hds = new HikariDataSource();
			hds.setDriverClassName(this.driverClassName);
			hds.setJdbcUrl(this.url);
			hds.setUsername(this.username);
			hds.setPassword(this.password);
			dataSource = hds;
		} else {
			dataSource = dataSourcePvd.getIfAvailable();
		}

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		return new WorkerNodeDAO(jdbcTemplate);
	}

	private WorkerNodeZK insWorkerNodeZK() {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
	    CuratorFramework cf = null;
	    if(StringUtils.isNotBlank(this.authentication)) {
	    	cf = CuratorFrameworkFactory.builder()
	    			.connectString(this.addrs)
	    			.sessionTimeoutMs(this.sessionTimeoutMs)
	    			.retryPolicy(retryPolicy)
	    			.authorization("digest",this.authentication.getBytes())
	    			.build();
	    }else {
	    	cf = CuratorFrameworkFactory.builder()
	                .connectString(this.addrs)
	                .sessionTimeoutMs(this.sessionTimeoutMs)
	                .retryPolicy(retryPolicy)
	                .build();
	    }
	    
	    cf.start();
	     
	    DistributedAtomicLong distAtomicLong = new DistributedAtomicLong(cf, COUNTER_ZNODE, retryPolicy);

		return new WorkerNodeZK(distAtomicLong);
	}

	@Bean
	public DisposableWorkerIdAssigner disposableWorkerIdAssigner() {
		return new DisposableWorkerIdAssigner();
	}

	@Bean
	public CachedUidGenerator cachedUidGenerator(DisposableWorkerIdAssigner workerIdAssigner) {
		CachedUidGenerator generator = new CachedUidGenerator();
		generator.setWorkerIdAssigner(workerIdAssigner);

		// 以下为可选配置, 如未指定将采用默认值
		if (this.timeBits > 0) {
			generator.setTimeBits(this.timeBits);
		}
		if (this.WorkerBits > 0) {
			generator.setWorkerBits(this.WorkerBits);
		}
		if (this.seqBits > 0) {
			generator.setSeqBits(this.seqBits);
		}
		if (StringUtils.isNotEmpty(this.epochStr)) {
			generator.setEpochStr(this.epochStr);
		}

		// RingBuffer size扩容参数, 可提高UID生成的吞吐量
		// 默认:3， 原bufferSize=8192, 扩容后bufferSize= 8192 << 3 = 65536
		if (this.boostPower > 0) {
			generator.setBoostPower(this.boostPower);
		}

		// 指定何时向RingBuffer中填充UID, 取值为百分比(0, 100), 默认为50
		// 举例: bufferSize=1024, paddingFactor=50 -> threshold=1024 * 50 / 100 = 512.
		// 当环上可用UID数量 < 512时, 将自动对RingBuffer进行填充补全
		if (this.paddingFactor > 0) {
			generator.setPaddingFactor(this.paddingFactor);
		}

		// 另外一种RingBuffer填充时机, 在Schedule线程中, 周期性检查填充
		// 默认:不配置此项, 即不实用Schedule线程. 如需使用, 请指定Schedule线程时间间隔, 单位:秒
		if (this.scheduleInterval > 0) {
			generator.setScheduleInterval(this.scheduleInterval);
		}

		// 拒绝策略: 当环已满, 无法继续填充时
		// 默认无需指定, 将丢弃Put操作, 仅日志记录. 如有特殊需求, 请实现RejectedPutBufferHandler接口(支持Lambda表达式)
		// <property name="rejectedPutBufferHandler"
		// ref="XxxxYourPutRejectPolicy"></property>
		// cachedUidGenerator.setRejectedPutBufferHandler();
		// 拒绝策略: 当环已空, 无法继续获取时 -->
		// 默认无需指定, 将记录日志, 并抛出UidGenerateException异常. 如有特殊需求,
		// 请实现RejectedTakeBufferHandler接口(支持Lambda表达式) -->
		// <property name="rejectedTakeBufferHandler"
		// ref="XxxxYourTakeRejectPolicy"></property>

		return generator;
	}

}
