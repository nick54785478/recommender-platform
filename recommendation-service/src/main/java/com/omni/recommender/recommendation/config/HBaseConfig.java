package com.omni.recommender.recommendation.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * HBase 連線配置 (The Dirtiest Layer)
 * 依照規範統一放置於根目錄的 config/ 下
 */
@org.springframework.context.annotation.Configuration
public class HBaseConfig {

    @Value("${hbase.zookeeper.quorum:localhost}")
    private String zookeeperQuorum;

    @Value("${hbase.zookeeper.property.clientPort:2181}")
    private String zookeeperClientPort;

    @Bean
    public Configuration hbaseConfiguration() {
        // 針對 Windows 開發環境的繞過機制 (避免 winutils.exe FileNotFoundException)
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            if (System.getProperty("hadoop.home.dir") == null && System.getenv("HADOOP_HOME") == null) {
                System.setProperty("hadoop.home.dir", new java.io.File(".").getAbsolutePath());
            }
        }
        
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", zookeeperQuorum);
        conf.set("hbase.zookeeper.property.clientPort", zookeeperClientPort);
        return conf;
    }
}
