# 全通路推薦平台 (Omni-Recommender Platform) 端到端測試指南

這份文件記錄了如何在本機端進行完整的端到端 (End-to-End, E2E) 測試，涵蓋了從「前端行為打點」到「Spark 大數據運算」，最後到「HBase 即時推薦查詢」的完整資料流。

## 🎬 測試步驟

### 1. 啟動基礎設施與微服務
* 確保 Docker 容器（Hadoop, Spark, HBase, Zookeeper）皆已啟動。
* 進入 HBase 建立推薦結果資料表（若尚未建立）：
  ```bash
  echo "create 'recommendation', 'cf'" | docker exec -i hbase-master hbase shell -n
  ```
* 在 IDE 中啟動 `behavior-service` (預設 Port: `9001`)。
* 在 IDE 中啟動 `recommendation-service` (預設 Port: `9002`)。

### 2. 寫入模擬行為日誌 (User Behavior)
使用 cURL 模擬使用者在前端 App 的點擊與購買行為：
```bash
# 寫入購買事件
curl -X POST http://localhost:9001/api/v1/behaviors/log \
     -H "Content-Type: application/json" \
     -d "{\"userId\":\"U001\", \"sessionId\":\"S001\", \"itemId\":\"ITEM_999\", \"behaviorType\":\"PURCHASE\"}"

# 寫入瀏覽事件
curl -X POST http://localhost:9001/api/v1/behaviors/log \
     -H "Content-Type: application/json" \
     -d "{\"userId\":\"U001\", \"sessionId\":\"S001\", \"itemId\":\"ITEM_888\", \"behaviorType\":\"VIEW\"}"
```
*預期結果：回傳 `202 ACCEPTED`。稍候一分鐘，系統會自動將日誌批次上傳至 HDFS。*

### 3. 執行大數據運算 (Spark ALS)
不要透過 IDE 的 Run 按鈕執行！請使用我們專門配置的 Maven 外掛來啟動，以確保 Java 22 的環境變數正確載入。
1. 開啟 IDE 的 Maven 面板。
2. 找到 `spark-recommender` -> `Plugins` -> `exec`。
3. 雙擊 **`exec:exec`** 執行批次任務。
*預期結果：Spark 成功從 HDFS 讀取日誌，訓練 ALS 模型，並將結果寫入 HBase。*

### 4. 獲取推薦結果 (Recommendation)
透過推薦微服務查詢最終結果：
```bash
curl -X GET http://localhost:9002/api/v1/recommendations/U001
```
*預期結果：回傳使用者的專屬推薦商品清單。*

---

## 💣 常見問題與排錯指南 (Troubleshooting)

在開發與測試這套複雜的分散式系統時，你可能會遇到以下經典地雷。我們已經將解法記錄下來：

### 1. 呼叫打點 API 時收到 `405 Method Not Allowed` 且畫面顯示 Jetty
* **原因**：呼叫到了錯誤的 Port (`8080`)。Docker 中的 Spark Master 預設佔用了 `8080` Port 並啟動了 Jetty 伺服器。
* **解法**：請確認呼叫的 Port 是否正確。我們的微服務分別配置在 `9001` (`behavior-service`) 與 `9002` (`recommendation-service`)。

### 2. 呼叫打點 API 時收到 `400 Bad Request`
* **原因**：Spring Boot 的 `@Valid` 驗證擋下了不合法的請求。
* **解法**：檢查 JSON Payload 是否遺漏了加上 `@NotBlank` 的必填欄位（例如 `sessionId`），或是欄位名稱打錯（例如把 `behaviorType` 錯打成 `action`）。

### 3. Spark 啟動時發生 `java.lang.NoSuchMethodException: java.nio.DirectByteBuffer.<init>` 
* **原因**：Java 版本過新 (Java 17/21/22)。舊版 Spark 3.0.0 依賴的底層記憶體存取 API 已被現代 Java 移除。
* **解法**：我們已在 `pom.xml` 中將 Spark 升級至支援現代 Java 的 `3.5.1` 版。

### 4. Spark 啟動時發生 `IllegalAccessError` (cannot access class sun.nio.ch.DirectBuffer)
* **原因**：即便升級 Spark，在 Java 17+ 仍需要開啟 JVM 模組後門 (`--add-opens`)。如果在 IDE 中直接點擊 Run，沒有帶上這些參數就會崩潰。
* **解法**：永遠透過 Maven 的 **`exec:exec`** 目標來啟動 Spark，我們已經將所有的 `--add-opens` 參數寫死在 `pom.xml` 的外掛設定中了。

### 5. 推薦服務啟動時跳出 `Hadoop bin directory does not exist` 或 `Bits#unaligned() check failed` 警告
* **原因**：Windows 環境缺少 `winutils.exe`，以及 Java 22 封鎖了 `Unsafe` 記憶體操作。
* **解法**：**直接忽略**。這兩個都是非致命警告，HBase Client 會自動降級使用安全模式運行，不影響系統功能。

### 6. 呼叫 API 或執行 Spark 時發生 `UnknownHostException: hbase-regionserver...` 或 `namenode`
* **原因**：Docker 網路迷宮。Zookeeper 或 HDFS 回傳了 Docker 內部的機器名稱給你的本機 Windows 電腦，導致 Windows 無法解析該網址。
* **解法**：以系統管理員身分修改 Windows 的 `C:\Windows\System32\drivers\etc\hosts` 檔案，在最底端加入：
  ```text
  127.0.0.1 hbase-regionserver.hadoopspark_hadoop-net
  127.0.0.1 namenode
  127.0.0.1 datanode
  127.0.0.1 hbase-master
  127.0.0.1 zookeeper
  ```

### 7. 執行 Spark 時發生 `BlockMissingException` 或 `Dead nodes`
* **原因**：大數據最經典的「IP 路由陷阱」！雖然我們已經把 `datanode` 加到 `hosts` 檔，但是 HDFS NameNode 預設會把 DataNode 的「Docker 內部 IP (192.168.x.x)」傳給 Spark，導致 Spark 連線失敗。
* **解法**：我們已經在 `RecommenderBatchJob.java` 的 Spark 初始化階段中，加入了一行魔法指令：`spark.sparkContext().hadoopConfiguration().set("dfs.client.use.datanode.hostname", "true");`，強迫 HDFS 使用主機名稱 (Hostname) 而非 IP 進行連線。

### 8. 呼叫推薦 API 成功，但回傳的 JSON 裡帶有 `"fallback":true`
* **原因**：這是 **預期內且完全正確的行為**！當 HBase 裡找不到該名用戶的推薦資料（可能因為 ALS 資料量太小無法訓練，或是 Spark 還沒執行），我們的領域驅動設計 (DDD) 機制發揮了作用。
* **解法**：無需解決。系統完美攔截了異常，並自動降級 (Fallback)，回傳了一組「預設熱門商品」給用戶，展現了微服務的絕佳容錯能力。
