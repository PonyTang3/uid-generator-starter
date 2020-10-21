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
 * ����
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

		// ����Ϊ��ѡ����, ��δָ��������Ĭ��ֵ
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

		// RingBuffer size���ݲ���, �����UID���ɵ�������
		// Ĭ��:3�� ԭbufferSize=8192, ���ݺ�bufferSize= 8192 << 3 = 65536
		if (this.boostPower > 0) {
			generator.setBoostPower(this.boostPower);
		}

		// ָ����ʱ��RingBuffer�����UID, ȡֵΪ�ٷֱ�(0, 100), Ĭ��Ϊ50
		// ����: bufferSize=1024, paddingFactor=50 -> threshold=1024 * 50 / 100 = 512.
		// �����Ͽ���UID���� < 512ʱ, ���Զ���RingBuffer������䲹ȫ
		if (this.paddingFactor > 0) {
			generator.setPaddingFactor(this.paddingFactor);
		}

		// ����һ��RingBuffer���ʱ��, ��Schedule�߳���, �����Լ�����
		// Ĭ��:�����ô���, ����ʵ��Schedule�߳�. ����ʹ��, ��ָ��Schedule�߳�ʱ����, ��λ:��
		if (this.scheduleInterval > 0) {
			generator.setScheduleInterval(this.scheduleInterval);
		}

		// �ܾ�����: ��������, �޷��������ʱ
		// Ĭ������ָ��, ������Put����, ����־��¼. ������������, ��ʵ��RejectedPutBufferHandler�ӿ�(֧��Lambda���ʽ)
		// <property name="rejectedPutBufferHandler"
		// ref="XxxxYourPutRejectPolicy"></property>
		// cachedUidGenerator.setRejectedPutBufferHandler();
		// �ܾ�����: �����ѿ�, �޷�������ȡʱ -->
		// Ĭ������ָ��, ����¼��־, ���׳�UidGenerateException�쳣. ������������,
		// ��ʵ��RejectedTakeBufferHandler�ӿ�(֧��Lambda���ʽ) -->
		// <property name="rejectedTakeBufferHandler"
		// ref="XxxxYourTakeRejectPolicy"></property>

		return generator;
	}

}
