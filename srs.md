# 軟體需求規格書 (SRS)：旅遊電商用戶行為分析與推薦系統

## 1. 簡介 (Introduction)

### 1.1 系統目的 (Purpose)
本系統旨在為旅遊電商平台建立一套基於大數據架構的「用戶行為分析與推薦系統」。透過收集用戶在平台上的瀏覽、搜尋與點擊行為，利用 Apache Spark 進行資料清洗與機器學習模型訓練，最終為每位用戶提供客製化的旅遊行程推薦，以提升用戶體驗與轉換率。

### 1.2 背景與範圍 (Scope)
*   **資料收集**：Spring Boot (AP) 接收前端日誌，先暫存於本地端 (Local Buffer/File)，再定時批次搬運至 Hadoop (HDFS)。
*   **離線運算**：使用 Apache Spark 定期 (每日) 進行批次處理，包含特徵工程與協同過濾 (Collaborative Filtering) 推薦模型訓練。
*   **低延遲查詢**：將計算完成的推薦結果寫入 HBase，提供前端毫秒級的查詢服務。
*   **數據分析**：將結構化數據存入 Hive，供後續 BI 報表與營運人員查詢。

---

## 2. 領域驅動設計：限界上下文 (Bounded Contexts)

基於 Domain-Driven Design (DDD) 的原則，本系統將劃分為以下幾個 Bounded Contexts，以確保微服務架構下的關注點分離與高內聚性：

### 2.1 用戶行為上下文 (User Behavior Context)
*   **職責**：專注於記錄與管理用戶在平台上的所有互動行為（例如：瀏覽、搜尋、加入購物車、購買）。
*   **介面**：提供 API 供前端或 Gateway 送入日誌資料。
*   **內部處理**：將接收到的請求轉換為領域事件，並落地至 AP 本地端（例如透過 Logback/Log4j2 Rolling File 產生結構化 JSON 檔案）。

### 2.2 推薦引擎上下文 (Recommendation Engine Context)
*   **職責**：負責提供用戶專屬的推薦行程清單，並保證查詢的高可用性與低延遲。
*   **介面**：對外提供 `GET /recommendations/{userId}` API。
*   **內部處理**：從 HBase (作為 Read Model) 中取得預先算好的推薦結果，並負責發生 Cache Miss 時的降級策略 (Fallback Strategies，例如回傳預設的熱門行程)。

### 2.3 數據分析與處理上下文 (Data Analytics & Pipeline Context)
*   **職責**：大數據管線，負責 ETL (Extract, Transform, Load) 與模型訓練。
*   **內部處理**：包含定時任務將 AP 端的本地日誌上傳至 HDFS，觸發 Spark Job 進行模型訓練與資料清洗，並將結果輸出至 HBase (供推薦上下文使用) 與 Hive (供報表分析)。

---

## 3. 系統架構設計 (System Architecture)

> [!NOTE]
> 本架構基於 Lambda 架構的離線批次處理層 (Batch Layer) 與服務層 (Serving Layer) 進行設計。

*   **API 應用層 (Java 22 + Spring Boot 4.1.0)**:
    *   `Behavior Logger`: 接收前端行為數據並寫入本地端硬碟。
    *   `Data Shipper`: Spring Boot 內建 `@Scheduled` 任務或掛載外部工具 (如 Flume)，定期將本地日誌傳送至 HDFS。
    *   `Recommendation API`: 連線至 HBase 提供推薦查詢。
*   **資料儲存層 (Storage)**:
    *   `Local FS`: AP 伺服器本地端暫存的 Rolling 檔案。
    *   `HDFS`: 儲存原始日誌檔案 (Raw Data)。
    *   `Hive`: 儲存清洗過後的結構化數據 (Data Warehouse)。
    *   `HBase`: 儲存 Key-Value 格式的推薦清單 (Serving Database)。
*   **運算處理層 (Compute)**:
    *   `Spark Core / Spark SQL`: 負責資料清洗 (ETL)。
    *   `Spark MLlib`: 負責訓練 ALS 推薦系統模型。
    *   `YARN`: 資源調度與管理。

---

## 4. 功能需求 (Functional Requirements)

### 4.1 數據採集與儲存 (Data Ingestion)
*   **REQ-1.1**: User Behavior Context 需提供接收 JSON 格式日誌的 API 端點。
*   **REQ-1.2**: 系統需先將日誌寫入 AP 本地端 (例如 `/var/log/app/behavior.log`)，避免直接寫入 HDFS 造成的網路延遲與阻塞。
*   **REQ-1.3**: 需實作批次上傳機制，例如每 15 分鐘將本地端 Rolling 產生的日誌檔案搬運至 HDFS 的 `/data/raw/user_behavior/YYYY-MM-DD/`。

### 4.2 離線數據處理與模型訓練 (Data Processing & ML)
*   **REQ-2.1**: Data Analytics Context 的 Spark Job 需讀取 HDFS 上的日誌，進行資料清洗與特徵工程。
*   **REQ-2.2**: Spark Job 需將清洗後的數據寫入 Hive 表，供後續查詢。
*   **REQ-2.3**: Spark MLlib 訓練協同過濾 (ALS) 推薦模型，計算 Top-N 推薦權重。

### 4.3 推薦服務提供 (Recommendation Serving)
*   **REQ-3.1**: Spark Job 需將產出的推薦結果批次寫入 HBase。
*   **REQ-3.2**: Recommendation Engine Context 提供前端根據 UserID 查詢推薦結果，底層需透過 HBase Java Client 獲取資料。

---

## 5. 非功能需求 (Non-Functional Requirements)

> [!IMPORTANT]
> 效能與可靠性是推薦系統服務化的關鍵。

*   **效能 (Performance)**:
    *   Recommendation API 的平均響應時間需小於 **100ms**。
    *   日誌採集 API 的平均響應時間需小於 **10ms**（得益於本地端寫入機制）。
*   **可擴展性 (Scalability)**:
    *   API 應用層可支援 Stateless 橫向擴展 (Scale-out)。
    *   大數據組件 (HDFS, YARN, HBase) 亦可水平擴充節點。

---

## 6. 技術堆疊 (Technology Stack)

*   **Backend Framework**: **Java 22, Spring Boot 4.1.0**
*   **Build Tool**: **Maven**
*   **Big Data Ecosystem** (基於現有 Docker Compose):
    *   Hadoop 3.2.1 (HDFS, YARN)
    *   Spark 3.0.0
    *   Hive 2.3.2 (Metadata in PostgreSQL)
    *   HBase latest (with ZooKeeper 3.8)

---

## 7. 驗證與後續計畫 (Verification Plan)

待 SRS 獲得核准後，將進入開發實作階段。
*   **Phase 1**: 建立 Spring Boot 4.1.0 專案架構，劃分 Bounded Contexts 套件結構。
*   **Phase 2**: 實作本地端日誌記錄與 HDFS 上傳機制 (Data Ingestion)。
*   **Phase 3**: 實作 HBase 寫入與 Recommendation API (Mock Data)。
*   **Phase 4**: 撰寫 Spark 應用程式並提交至叢集運算 (ETL & ML)，串接端到端流程。
