# Spark Recommender Module 🧠

本模組 (`spark-recommender`) 是 Omni-Recommender Platform 的**「大數據運算與機器學習核心」**。它負責在背景執行離線批次運算 (Offline Batch Processing)，將使用者產生的海量行為日誌轉化為精準的個人化推薦清單。

## 🎯 模組用途 (Purpose)

此模組扮演了連結「資料湖 (HDFS)」與「線上服務 (HBase)」的橋樑。它的主要工作流程如下：
1. **資料萃取 (Extract)**：從 HDFS 讀取由 `behavior-service` 上傳的原始使用者行為日誌 (`.json.gz`)。
2. **資料轉換 (Transform)**：將非結構化的 JSON 日誌解析並清理，轉換為適合機器學習演算法的「使用者-物品評分矩陣 (User-Item Rating Matrix)」。
3. **模型訓練 (Train)**：將資料餵給 AI 模型進行訓練，計算出每個使用者最可能感興趣的潛在商品。
4. **資料載入 (Load)**：將計算完成的「Top-N 推薦清單」直接寫入 **Apache HBase**，讓前端的 `recommendation-service` 能夠以毫秒級的速度進行即時查詢。

---

## 🤖 AI 模型：ALS 協同過濾演算法

本模組採用的是 Apache Spark MLlib 內建的 **ALS (Alternating Least Squares, 交替最小平方法)** 模型。

### 什麼是 ALS 模型？
ALS 是一種非常經典且強大的**協同過濾 (Collaborative Filtering)** 演算法，屬於**矩陣分解 (Matrix Factorization)** 的一種技術。
廣泛應用於 Netflix、Amazon 等大型平台的推薦系統中。

### 它是如何運作的？
1. **隱含特徵 (Latent Features)**：ALS 假設使用者的喜好和商品的特性，都可以由一組隱藏的特徵向量來表示。
2. **矩陣分解**：它會試圖將我們龐大且稀疏的「使用者評分矩陣」，拆解成兩個較小的矩陣：「使用者特徵矩陣」與「商品特徵矩陣」。
3. **交替優化 (Alternating)**：在訓練過程中，它會先固定「商品矩陣」來優化「使用者矩陣」，接著再固定「使用者矩陣」來優化「商品矩陣」。兩者不斷交替迭代，直到找出誤差最小的最佳解。
4. **精準預測**：訓練完成後，只要將某個使用者的特徵向量與某個商品的特徵向量相乘，就能預測出該使用者對該商品有多感興趣！

### 為什麼選擇 ALS？
* **適合隱式回饋 (Implicit Feedback)**：我們的系統收集的是「點擊 (CLICK)」、「購買 (PURCHASE)」等行為，而非直接的 1~5 星評分。ALS 模型對於處理這類隱式回饋有極佳的效果。
* **分散式運算**：ALS 演算法天生適合在 Spark 這種分散式運算叢集上平行處理，面對 TB 級的資料量也能游刃有餘。

---

## 🚀 如何執行

在開發環境下，可以透過 Maven 指令觸發 Spark Local Mode 的批次訓練：

```bash
cd spark-recommender
mvn compile
mvn exec:exec
```

> **注意**：執行前請確保 Docker 叢集中的 Hadoop (HDFS) 與 HBase 皆已啟動，並且 HDFS 內有可以供訓練的行為日誌。
