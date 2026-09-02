# 領域模型設計：Recommendation Service - Recommendation Aggregate

本文件定義了 `recommendation-service` 微服務核心的聚合根 (Aggregate Root) `Recommendation` 及其附屬實質物件 (Value Objects) 的詳細設計。

本微服務嚴格遵守 `AGENTS.md` 規範：
- 領域層內部**零框架侵入** (不使用 Lombok、JPA 或任何外部依賴)。
- 實質物件 (Value Object) 均採用 Java `record` 實作以確保不可變性 (Immutability)。

---

## 1. Aggregate 內部領域物件總覽 (Domain Objects)

| 領域物件名稱 (Domain Object) | 類型 (Stereotype) | 包含屬性 / 內容 (Properties) | 說明與職責 (Description) |
| :--- | :--- | :--- | :--- |
| **`Recommendation`** | **Aggregate Root** (聚合根) | `userId` (UserId)<br>`items` (List&lt;RecommendedItem&gt;)<br>`generatedAt` (Instant)<br>`isFallback` (boolean) | 負責封裝並管理單一用戶的專屬推薦清單。提供工廠方法以進行重建 (`restore`) 或降級 (`fallback`)。 |
| **`UserId`** | Value Object (Record) | `value` (String) | 封裝用戶 ID，並在建構時確保字串不得為 null 或空白。 |
| **`RecommendedItem`** | Value Object (Record) | `itemId` (String)<br>`score` (double)<br>`rank` (int) | 封裝單一推薦行程 / 商品的資訊。在建構時確保 `itemId` 有效且 `rank` 大於等於 1。 |

---

## 2. Aggregate 欄位定義與職責 (Field Definitions)

針對 `Recommendation` 聚合根內部的屬性詳細定義如下：

| 欄位名稱 (Field) | 型別 (Type / Value Object) | 類別 (Category) | 說明與職責 (Description) |
| :--- | :--- | :--- | :--- |
| **`userId`** | `UserId` | 識別碼 (Identity) | 擁有此推薦清單的使用者。 |
| **`items`** | `List<RecommendedItem>` | 核心資料 (Core) | 該用戶專屬的推薦項目清單，按推薦順序或分數排列。建立後即具有不可變性 (`List.copyOf()`)。 |
| **`generatedAt`** | `Instant` | 上下文 (Context) | 此推薦結果的產生時間（通常由 Spark 訓練完畢寫入 HBase 的時間，或是 Fallback 觸發當下的時間）。 |
| **`isFallback`** | `boolean` | 狀態指標 (State) | 標記此推薦清單是否為「快取未命中 (Cache Miss)」時所觸發的降級熱門行程。對於後續的成效追蹤與 A/B Testing 相當重要。 |

---

## 3. 業務規則與不變量驗證 (Invariants Validation)

1. **不可變性防護 (Immutability)**：
   - 聚合根內的 `items` 集合在傳入時會透過 `List.copyOf()` 進行保護，避免外部意外修改集合內容。
2. **建構與工廠方法 (Factory Methods)**：
   - `restore(UserId, List<RecommendedItem>, Instant)`：用於從資料庫/HBase 將已經計算好的推薦模型還原到記憶體中。此時 `isFallback` 恆為 `false`。
   - `fallback(UserId, List<RecommendedItem>)`：用於當 HBase 查詢不到專屬資料時，塞入預設的熱門推薦項目。此時會自動帶入當下時間 (`Instant.now()`) 並且 `isFallback` 恆為 `true`。
3. **Value Object 自我驗證**：
   - `UserId`：不可為空字串。
   - `RecommendedItem`：`itemId` 不可為空字串，且排序 `rank` 必須 $\ge 1$，若違反將拋出 `IllegalArgumentException` 阻斷建立過程。
