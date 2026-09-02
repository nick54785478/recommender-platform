package com.omni.recommender.behavior.presentation.scheduler;

import com.omni.recommender.behavior.application.port.out.BehaviorHdfsShipperPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 負責定時觸發 HDFS 上傳任務 (Driving Adapter / Inbound)
 * 在 Clean Architecture 中，Scheduler 屬於主動觸發系統的 Presentation/Inbound Layer。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HdfsUploadScheduler {

    private final BehaviorHdfsShipperPort hdfsShipperPort;

    // 每 15 分鐘執行一次 (15 * 60 * 1000 = 900000 ms)
    @Scheduled(fixedDelay = 10000)
    public void scheduleHdfsUpload() {
        log.info("Starting scheduled HDFS log shipping task...");
        hdfsShipperPort.shipLocalLogsToHdfs();
        log.info("Finished scheduled HDFS log shipping task.");
    }
}
