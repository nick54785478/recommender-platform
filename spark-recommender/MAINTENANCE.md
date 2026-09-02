# Spark Recommender 維護與營運手冊 (Maintenance Guide)

這份文件旨在幫助資料工程師 (Data Engineer) 與後端開發人員快速了解 `spark-recommender` 模組的運作原理、維護方式以及常見的排錯指南。

---

## 1. 模組定位與職責 (Module Purpose)

`spark-recommender` 是整個 Omni Recommender Platform 的「大腦」與離線運算核心 (Batch Layer)。
它是一個標準的 Apache Spark 應用程式，專門處理以下四大任務：
1. **資料載入 (Data Ingestion)**：從 HDFS 讀取使用者日誌。
2. **特徵工程 (Feature Engineering)**：清洗資料、產生模型所需特徵。
3. **模型訓練 (Model Training)**：使用 Spark MLlib 訓練 ALS (Alternating Least Squares) 協同過濾模型。
4. **結果匯出 (Data Sink)**：將推薦清單批次寫入 HBase 供 API 查詢。

---

## 2. 核心參數與商業邏輯配置

若未來商業邏輯發生改變，營運人員應重點維護以下程式碼區塊：

### 2.1 隱式回饋權重 (Implicit Feedback Weights)
位於 `FeatureEngineering.java` 中。因為系統沒有「星級評分」，我們依據用戶行為強弱賦予權重：
*   `VIEW` = 1.0 (瀏覽)
*   `CLICK` = 2.0 (點擊)
*   `SEARCH` = 2.5 (搜尋)
*   `LIKE` = 3.0 (收藏/按讚)
*   `ADD_TO_CART` = 4.0 (加入購物車)
*   `PURCHASE` = 5.0 (購買)
> **維護提醒**：若未來新增行為 (例如：分享 `SHARE`)，請務必於此處新增 `when()` 條件以納入模型評估。

### 2.2 ALS 模型參數 (ALS Hyperparameters)
位於 `AlsRecommenderTrainer.java` 中：
*   `maxIter` (預設 10)：迭代次數，調高能增加準確度但會拉長運算時間。
*   `regParam` (預設 0.01)：正規化參數，用於防止過度擬合 (Overfitting)。
*   `implicitPrefs` (預設 true)：**絕對不可關閉**，這是專門處理無明確評分行為的核心開關。
*   `coldStartStrategy` (預設 drop)：丟棄冷啟動資料，交由後端 API 的 Fallback 機制處理。

---

## 3. 建置與部署 (Build & Deployment)

### 3.1 打包成 Fat JAR
為了讓 Spark 叢集能夠正確執行，我們在 `pom.xml` 中配置了 `maven-assembly-plugin`，會將所有依賴一起打包：
```bash
# 在專案根目錄下執行 (omni-recommender-platform)
mvn clean package -pl spark-recommender
```
產出的檔案會位於：`spark-recommender/target/spark-recommender-1.0.0-SNAPSHOT-jar-with-dependencies.jar`

### 3.2 透過 Spark-Submit 執行 (生產環境)
在生產環境 (如 Hadoop YARN) 中，請使用以下指令提交任務：
```bash
# 若在 Windows CMD 執行，請務必複製為「單行」避免換行字元 (backslash) 解析錯誤：
spark-submit --class com.omni.recommender.spark.RecommenderBatchJob --master yarn --deploy-mode cluster --executor-memory 2G --num-executors 2 spark-recommender/target/spark-recommender-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

### 3.3 本地端開發與執行 (Local Development & Execution)
在本地電腦開發或除錯時，你可以完全不需要安裝完整的 Hadoop 叢集，只需要：
1. **直接透過 IDE (IntelliJ IDEA / Eclipse) 執行**：
   這是最適合「日常開發與除錯 (Debug)」的方式，因為你可以直接下中斷點 (Breakpoint) 觀察 DataFrame 的資料流轉與變化。
   由於 IDE 預設沒有 Hadoop YARN 叢集環境，你需要明確告訴 Spark 啟動「本地模擬模式」。

   **具體作法有兩種 (擇一即可)**：
   * **作法 A (修改程式碼)**：
     打開主程式 `RecommenderBatchJob.java`，在初始化 `SparkSession` 的地方手動加上 `.master("local[*]")`（`*` 代表使用你電腦上所有可用的 CPU 核心來模擬叢集）：
     ```java
     SparkSession spark = SparkSession.builder()
             .appName("OmniRecommender-ALS-Job")
             .master("local[*]") // <--- 開發時加上這一行
             .getOrCreate();
     ```
   * **作法 B (修改 IDE 的 Run Configuration)**：
     如果你不希望更動到程式碼（避免不小心 Commit 到生產環境），你可以在 IntelliJ 的右上角開啟 `Run/Debug Configurations`，在 `VM Options` (新版 IntelliJ 請點 `Modify options` -> `Add VM options`) 欄位中，填入以下系統參數：
     ```text
     -Dspark.master=local[*]
     ```
   設定完成後，只要在 `RecommenderBatchJob.java` 按下右鍵選擇 `Run main()` 或 `Debug main()`，就能順利跑起整個大數據管線了！
2. **透過終端機執行 Fat JAR**：
   如果你已經打包好，也可以直接當作一般的 Java 程式來執行：
   ```bash
   java -Dspark.master=local[*] \
        -cp spark-recommender/target/spark-recommender-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
        com.omni.recommender.spark.RecommenderBatchJob
   ```
   > 提醒：若在 Windows 上遭遇 `winutils` 問題，請參考下方的 Q1 排錯指南。

---

## 4. 常見問題與排錯指南 (Troubleshooting)

### Q1: 在 Windows 本地端開發時拋出 `java.io.FileNotFoundException: HADOOP_HOME and hadoop.home.dir are unset.`
* **原因**：Hadoop 核心庫在 Windows 作業系統上需要依賴 `winutils.exe` 來檢查檔案權限。
* **解法**：這是本地開發限定的錯誤。請上網下載對應 Hadoop 版本的 `winutils.exe`，放入任一資料夾 (例如 `C:\hadoop\bin`)，然後在系統環境變數中設定 `HADOOP_HOME` 指向 `C:\hadoop`。若佈署到 Linux 生產環境則不會遇到此問題。

### Q2: 拋出 `org.apache.hadoop.hbase.client.RetriesExhaustedException`
* **原因**：Spark 無法連線到 HBase 的 Zookeeper 或 RegionServer。
* **解法**：檢查 `HBaseWriter.java` 中呼叫的 `zookeeperQuorum` 參數，確認 Spark Executor 節點是否能正確解析該 Hostname (例如 `zookeeper`) 並且能連通 Port `2181`。

### Q3: 拋出 `java.lang.IllegalArgumentException: requirement failed: ALS requires rating/user/item columns to be numeric`
* **原因**：你的 `userId` 或 `itemId` 是字串型別，未經轉換就直接丟給 ALS。
* **解法**：確認 `FeatureEngineering` 中的 `StringIndexer` 是否有被正確執行。ALS 模型**只能吃 Integer 或是 Long 型別的 ID**。
