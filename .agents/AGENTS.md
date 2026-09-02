# WorkSpace 開發規範與架構準則 (AGENTS.md)

本工作區（WorkSpace）內的所有後端專案與開發任務，必須遵循以下三大架構設計與命名規範：

## 1. Domain Driven Design (DDD, 領域驅動設計)
- **核心領域至上**：領域模型（Domain Model）須封裝完整的商業規則與領域邏輯（例如 Aggregate Root 聚合根、Value Object 實質物件與 Domain Event 領域事件）。
- **零框架與零外層依賴**：`domain/` 層內的程式碼為純 Java（Pure Java），**絕對不可**依賴 Spring 框架、JPA 註解、資料庫相關技術，亦**嚴禁**依賴外層的 `application/` 或 `infrastructure/` 套件。
- **嚴格禁止技術類侵入 Domain Layer**：`domain/` 層內部嚴禁引入或依賴任何框架、資料庫 ORM（如 JPA `Entity`、`Table` 註解、Hibernate）、序列化套件（如 Jackson `@JsonProperty`）、HTTP 協定物件或任何外部基礎設施相關套件，保證領域邏輯的純粹與技術無關性。
- **領域事件驅動**：當領域狀態變更時，經由 Aggregate 觸發並對外發布對應的 Domain Event。
- **垂直切片與 Aggregate 聚合 (Vertical Slice Architecture)**：
  - Domain Layer 內部**必須依據 Aggregate (聚合根)** 進行切片與封裝（例如 `domain/task/` 作為 Aggregate 邊界），將該 Aggregate 專屬的 `aggregate/`（Aggregate Root 與 Value Objects）、`event/`（Domain Events）與 `exception/`（領域例外）內聚於此，嚴禁散落於全域共用目錄。
## 2. Clean Architecture & 六角形架構 (Hexagonal Architecture / Ports & Adapters)
後端程式碼須依據分層結構清楚隔離，並明確規範 **Port 與 Adapter 的放置規則**：
- **Port 方法嚴禁接發技術類物件**：所有 Input Port (`port/in/`) 與 Output Port (`port/out/`) 介面的方法，**傳參及回傳型別嚴禁使用任何技術類或環境相依的物件**（如 ORM 實體 `TaskJpaEntity`、資料庫連線 `Connection`、HTTP 請求/回應物件 `HttpServletRequest` / `ResponseEntity` 等）；Port 僅可接收並回傳領域物件（Aggregate Root, Value Object）、Command、Query、DTO 與 Projection。
- **業務功能必經 UseCase (Inbound Port)**：所有業務功能與應用操作，**必定要定義並實作對應的 Inbound Port (`... + UseCase` 介面)**，再由 `application/service/` 內的 `ApplicationService` (或 `CommandService`/`QueryService`) 來實作，嚴禁 Controller 或呼叫端直接操作 Domain 或 Repository。
- **Adapter 存取權限與調用規則 (Package-Private)**：所有 Adapter 類別（含 Inbound Application Service 與 Outbound Adapter，如 `TaskRepositoryAdapter`、`TaskMessagePublisherAdapter`、`TaskApplicationService`）應宣告為 **package-private (預設存取修飾詞，不加上 `public`)**，嚴禁於外層直接建構或直接參考該 Adapter 實體類別；所有對外調用必須透過依賴注入其實作的 **Port 介面**（或 `UseCase` 介面）進行。
- **Domain Layer (核心領域層 - Pure Domain)**：
  - 以 Aggregate 為頂層目錄（例如 `domain/task/`）：
    - `aggregate/`：該 Aggregate 的領域模型，內部須進一步細分為 `root/` (Aggregate Root 聚合根，如 `Task`)、`entity/` (內部實體，如 `TaskStep`) 與 `vo/` (Value Objects 實質物件，如 `TaskId`, `TaskStatus`)。
    - `event/`：該 Aggregate 觸發的純領域事件 (例如 `TaskCreatedEvent`, `TaskUpdatedEvent`)。
    - `exception/`：該 Aggregate 專屬的領域例外（例如 `TaskNotFoundException`）。
- **Application Layer (應用層)**：
  - 負責 CQRS 流程調度、Command/Query 物件、Dto 與 View Projection。
  - **Dto 與 View 嚴格作為純資料載體**：Dto (`TaskGottenResult`) 與 View (`TaskGottenView`) 不得負責轉換工作（嚴守單一職責，禁止在 Dto 內部寫入 `fromDomain` 等轉換邏輯）；領域模型對 Dto/View 的轉換須由獨立於 `dto/` 之外的 `assembler/` 套件中的 Assembler / Mapper 物件（例如 `TaskDtoAssembler`）負責執行。
  - **Input Port 與 Output Port 統一放置於 Application 層 (`port/`)**：
    - `port/in/` (Inbound Ports / Use Cases)：代表應用層對外開放的業務功能介面（如 `CreateTaskUseCase`、`UpdateTaskUseCase`、`GetTaskUseCase`），接收 Command/Query 並回傳 DTO/View。
    - `port/out/` (Outbound Ports)：代表應用層對外要求環境設施協同的介面，放置 JPA 持久化、MQ 訊息隊列、快取或外部服務介面（例如 `TaskRepositoryPort`、`TaskMessagePublisherPort`）。
  - `service/`：實作 `port/in/` 中的 UseCase 介面，協調 Domain Aggregate 與 Application Outbound Port (`port/out/`)。
- **Infrastructure Layer (基礎設施層 - Outbound Adapters)**：
  - **模組分離與聚合設計**：
    - `persistence/`：直接位於 `infrastructure/` 下，內部**須依據 Aggregate 垂直切片**（如 `persistence/task/`），並進一步區分為 `entity/`（如 ORM 實體 `TaskJpaEntity`）與 `repository/`（如 Spring Data Repository `TaskJpaRepository`）。
    - `messaging/`：直接位於 `infrastructure/` 下，放置訊息處理或外部 Broker 通訊模組（如 `TaskMqProducer`）。
    - `adapter/`：作為**所有基礎設施模組聚合的地方**，放置實作 Application 層 Outbound Port 的適配器（如 `TaskRepositoryAdapter`、`TaskMessagePublisherAdapter`），在此處調用並聚合 `persistence/`、`messaging/` 等其他模組。
- **Presentation Layer (表現層 - Inbound Adapters)**：
  - REST Controller 或其他對外 API 介面，接收 Request 轉換為 Command/Query，呼叫應用層入站 UseCase 並將結果封裝為對應 Response。
  - **Resource 嚴格作為純資料載體與 Assembler 防腐轉換 (`assembler/`)**：
    - Request/Response Resource 物件僅維持嚴格的純資料載體（Pure Data Carrier），**不得在 Resource 內部實作 `toCommand` 或 `fromResult` 等任何轉換邏輯**。
    - 表現層與應用層之間的資料轉換與防腐處理 (Anti-Corruption Layer)，**全權交由獨立於 `resource/` 之外的 `assembler/` 套件中 Assembler / Mapper 物件（例如 `TaskResourceAssembler`）負責執行**。
  - **Resource 分層規則 (`resource/`)**：
    - `resource/in/` (Request Data)：對外接收的請求載體 (`V + N + Resource`，如 `CreateTaskResource`、`UpdateTaskResource`)。
    - `resource/out/` (Response Data)：對外回傳的結果載體 (`N + Ved + Resource`，如 `TaskCreatedResource`、`TaskRetrievedResource`)。
- **Configuration Layer (配置層 - The Dirtiest Layer)**：
  - **全域配置獨立存放**：所有的 Spring Configuration (`@Configuration`)、Bean 註冊、Web/WebSocket 設定、Security 等技術耦合度極高的配置檔，**一律統一放置於專案根目錄下的 `config/` 套件中**。
  - 因為它是系統最外圍、技術細節最髒 (Dirtiest) 的一層，專門負責將所有的基礎設施與框架組裝起來，**嚴禁**將配置檔散落於 Domain、Application 或各個分層模組之內。

## 3. 嚴格架構與物件命名規範 (9 大約束)

在定義任何 DTO、請求、回應、事件、命令、查詢、視圖以及 Port 與 Adapter 時，必須嚴格按照以下命名規範：

| 類別 | 命名公式 | 說明與範例 |
| --- | --- | --- |
| **3-1. Request** | `V + N + Resource` | 動詞 + 名詞 + Resource (例如：`CreateTaskResource`, `UpdateTaskResource`) |
| **3-2. Response** | `N + Ved + Resource` | 名詞 + 過去分詞 + Resource (例如：`TaskCreatedResource`, `TaskUpdatedResource`, `TaskRetrievedResource`) |
| **3-3. Event** | `N + Ved + Event` | 名詞 + 過去分詞 + Event (例如：`TaskCreatedEvent`, `TaskUpdatedEvent`) |
| **3-4. Command** | `V + N + Command` | 動詞 + 名詞 + Command (例如：`CreateTaskCommand`, `UpdateTaskCommand`) |
| **3-5. Query** | `V + N + Query` | 動詞 + 名詞 + Query (例如：`GetTaskQuery`, `ListTaskQuery`) |
| **3-6. Dto** | `N + Gotten/Searched + Result` | 名詞 + 查詢類過去分詞 (如 Gotten, Searched 等) + Result (例如：`TaskGottenResult`, `TaskSearchedResult`) |
| **3-7. Projection** | `N + Gotten/Searched + View` | 名詞 + 查詢類過去分詞 (如 Gotten, Searched 等) + View (例如：`TaskGottenView`, `TaskSearchedView`) |
| **3-8. Port** | `in` : `... + UseCase`<br>`out` : `... + Port` | Inbound Port 須以 `UseCase` 結尾 (如 `CreateTaskUseCase`)；Outbound Port 須以 `Port` 結尾 (如 `TaskRepositoryPort`) |
| **3-9. Adapter** | `in` : `... + ApplicationService` (沒有 CQRS) 或 `... + CommandService` / `... + QueryService` (CQRS)<br>`out` : `... + Adapter` | Inbound Adapter 應用服務為 `ApplicationService` / `CommandService` / `QueryService` (如 `TaskApplicationService`)；Outbound Adapter 須以 `Adapter` 結尾 (如 `TaskRepositoryAdapter`) |

## 4. 程式碼註解與文檔規範 (Javadoc & Comments)
- **極致清晰的註解與 Javadoc**：專案內的所有類別、介面、實體物件、DTO、Port、Adapter 以及其內部所有的核心與業務方法，**盡可能加上清楚的 Javadoc 與註解**。
- **說明業務意圖與職責**：Javadoc 與註解須說明該類別或方法在 DDD / 六角形架構中的具體業務職責、參數與回傳值的意義（如 `@param`, `@return`、說明發布的領域事件或拋出的例外等），確保任何開發者或 AI 助手皆能迅速理解業務邏輯與架構意圖。

## 5. 測試規範與 UseCase 測試要求 (Testing & Verification)
- **UseCase 必備單元測試 (Mandatory UseCase Tests)**：每一個實作的 `UseCase` 業務功能（如 `CreateTaskUseCase`, `UpdateTaskUseCase` 等），**必定要撰寫對應的單元測試或整合測試 (Unit / Integration Tests)**，完整測試正常的業務流程、狀態異動、事件發布及例外情況（如 `TaskNotFoundException` 等），確保每個 UseCase 功能運作正確。
- **測試通過驗證**：開發與修改程式碼完成後，須確保所有單元與整合測試全數通過 (`BUILD SUCCESS`)。


