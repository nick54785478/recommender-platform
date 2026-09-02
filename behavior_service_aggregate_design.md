# 領域模型設計：Behavior Service - UserBehavior Aggregate

本文件定義了 `behavior-service` 微服務核心的聚合根 (Aggregate Root) `UserBehavior` 及其附屬物件的詳細設計。
為保持系統通用性（Omni-platform），領域模型將行業特定的資料抽離，統一由 `Metadata (BehaviorContext)` 進行彈性擴充。

---

## 1. Aggregate 內部領域物件總覽 (Domain Objects)

我們使用 Java Record 實作所有的 Value Object 以保證不可變性 (Immutability)，並移除對 Lombok 的依賴，以確保領域層的絕對純粹。

| 領域物件名稱 (Domain Object) | 類型 (Stereotype) | 包含屬性 / 內容 (Properties) | 說明與職責 (Description) |
| :--- | :--- | :--- | :--- |
| **`UserBehavior`** | **Aggregate Root** (實體) | `behaviorId` (String)<br>`userId` (UserId)<br>`sessionId` (SessionId)<br>`itemId` (ItemId)<br>`behaviorType` (Enum)<br>`timestamp` (Instant)<br>`deviceInfo` (DeviceInfo)<br>`referrerUrl` (String)<br>`metadata` (BehaviorContext) | 負責封裝並管理單一用戶行為紀錄，保護領域不變量 (Invariants)，也是外部存取該聚合的唯一入口。 |
| **`UserId`** | Value Object (Record) | `value` (String) | 封裝用戶 ID，並確保在建構時字串不得為空。 |
| **`SessionId`** | Value Object (Record) | `value` (String) | 封裝工作階段 ID，用於前端/App 的連貫行為追蹤。 |
| **`ItemId`** | Value Object (Record) | `value` (String) | 封裝被互動的目標物件 ID。 |
| **`BehaviorType`** | Enum | `VIEW`, `CLICK`, `LIKE`, `ADD_TO_CART`, `PURCHASE`, `SEARCH` | 定義合法的行為類型列舉。 |
| **`DeviceInfo`** | Value Object (Record) | `clientIp` (String)<br>`userAgent` (String) | 封裝用戶當下的連線與裝置環境資訊。 |
| **`BehaviorContext`** | Value Object (Record) | `data` (Map<String, String>) | 彈性的資料載體 (Metadata)，負責裝載特定行為專屬的不定結構資料。 |

---

## 2. Aggregate 欄位定義與職責 (Field Definitions)

針對 `UserBehavior` 聚合根內部的屬性詳細定義如下：

| 欄位名稱 (Field) | 型別 (Type / Value Object) | 類別 (Category) | 說明與職責 (Description) |
| :--- | :--- | :--- | :--- |
| **`behaviorId`** | `String` (UUID) | 識別碼 (Identity) | 系統內部唯一識別碼，建構時自動產生。 |
| **`userId`** | `UserId` | 關聯 (Relation) | 觸發此行為的會員 ID。若未登入，可為 null。 |
| **`sessionId`** | `SessionId` | 關聯 (Relation) | 追蹤同一次的工作階段，對計算轉換漏斗、短期意圖與停留時間至關重要。 |
| **`itemId`** | `ItemId` | 關聯 (Relation) | 該行為所關聯的商品、行程或頁面 ID。 |
| **`behaviorType`** | `BehaviorType` | 核心屬性 (Core) | 行為的本質 (例如：VIEW, CLICK)。 |
| **`timestamp`** | `Instant` | 核心屬性 (Core) | 行為發生的精確時間。若外部未傳入則系統自動代入。 |
| **`deviceInfo`** | `DeviceInfo` | 上下文 (Context) | 記錄發出請求的裝置環境，可用於進階推薦特徵與防詐欺。 |
| **`referrerUrl`** | `String` | 上下文 (Context) | 來源網址，記錄用戶是從哪裡點擊進入該頁面的。 |
| **`metadata`** | `BehaviorContext` | 彈性擴充 (Extension) | 用於記錄特定行為才具備的資訊 (如 SEARCH 帶有 `keyword`)。任何不屬於通用的行業特定資料 (如 ttravel 專屬欄位) 皆應放置於此。 |

---

## 3. 業務規則與不變量驗證 (Invariants Validation)

在 `UserBehavior` 聚合根的建構與工廠方法中，嚴格遵守以下領域規則：
1. **時間戳記保證**：若建立行為記錄時未提供時間，則領域層會自動帶入當下時間 (`Instant.now()`)，保證日誌必有時間戳記。
2. **Search 規則**：若 `behaviorType` 為 `SEARCH`，則 `metadata` 內必須包含 `keyword` 鍵值，否則建構過程將拋出 `IllegalArgumentException` (Domain Exception)。
3. **無效資料處理**：若外部未傳入 Metadata，領域層會自動建立一個空的 `BehaviorContext` 紀錄，避免後續取值時發生 `NullPointerException`。
