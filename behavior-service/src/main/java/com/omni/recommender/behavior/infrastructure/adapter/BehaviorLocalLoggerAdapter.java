package com.omni.recommender.behavior.infrastructure.adapter;

import com.omni.recommender.behavior.application.port.out.BehaviorLocalLoggerPort;
import com.omni.recommender.behavior.domain.behavior.aggregate.root.UserBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 將用戶行為日誌寫入本地硬碟的 Adapter
 * (依據規範：Adapter 必須位於 infrastructure/adapter/ 套件，且為 package-private)
 */
@Component
class BehaviorLocalLoggerAdapter implements BehaviorLocalLoggerPort {

    // 取得專門負責寫入行為日誌的 Logger，此 Logger 會在 logback-spring.xml 中被對應到 AsyncAppender
    private static final Logger BEHAVIOR_LOGGER = LoggerFactory.getLogger("BEHAVIOR_FILE_LOGGER");

    @Override
    public void writeLog(UserBehavior userBehavior) {
        // --- 1. 提取並安全處理可能為 Null 的基礎欄位 ---
        // 使用 Record 的原生方法 (如 value()) 來取得內容。若未登入或無 Session，則給予空字串避免 NullPointerException。
        String uid = userBehavior.getUserId() != null ? userBehavior.getUserId().value() : "";
        String sid = userBehavior.getSessionId() != null ? userBehavior.getSessionId().value() : "";
        String iid = userBehavior.getItemId() != null ? userBehavior.getItemId().value() : "";
        String ip = userBehavior.getDeviceInfo() != null ? userBehavior.getDeviceInfo().clientIp() : "";
        String ua = userBehavior.getDeviceInfo() != null ? userBehavior.getDeviceInfo().userAgent() : "";
        
        // --- 2. 處理動態擴充資料 (Metadata) ---
        // 將 Map 轉換為 JSON 格式字串。這裡為了展現底層運作並追求極致效能，採用了簡單的 toString() 轉換。
        // 實務上，若 Map 結構較為複雜，建議改用 Jackson 的 ObjectMapper (e.g. objectMapper.writeValueAsString(data)) 
        // 確保內部的雙引號與特殊字元能被正確轉義 (Escape)。
        String metaStr = userBehavior.getMetadata() != null && userBehavior.getMetadata().data() != null 
                         ? userBehavior.getMetadata().data().toString() 
                         : "{}";
                         
        // --- 3. 拼裝最終的 JSON 日誌字串 ---
        // 使用 String.format 來組合最終的一行 JSON (JSONL 格式)。
        // 這種一行一筆 JSON 的格式非常適合後續被 Hadoop/Spark/ELK 叢集直接消化解析。
        String jsonLog = String.format("{\"behaviorId\":\"%s\",\"userId\":\"%s\",\"sessionId\":\"%s\",\"itemId\":\"%s\",\"behaviorType\":\"%s\",\"ip\":\"%s\",\"ua\":\"%s\",\"timestamp\":\"%s\",\"metadata\":\"%s\"}",
                userBehavior.getBehaviorId(),
                uid, sid, iid,
                userBehavior.getBehaviorType().name(),
                ip, ua,
                userBehavior.getTimestamp().toString(),
                metaStr.replace("\"", "\\\"")); // 針對 metadata 內部的引號進行簡單防呆轉義
                
        // --- 4. 非同步寫入本地檔案 ---
        // 透過 Slf4j 寫入，實際上會交由 Logback 的 AsyncAppender 處理，達成對主業務執行緒極低延遲的目標。
        BEHAVIOR_LOGGER.info(jsonLog);
    }
}
