# mini-cob

一個仿照 [Apache Fineract](https://github.com/apache/fineract) 日終批次（Close of Business, COB）的小型 Spring Boot 專案，用來練習 Spring 交易、並發與 Spring Batch。

這個版本裡埋了 11 個 bug，每一個都對應 `exercises/` 底下的一道題目和一個會失敗的測試。另外還有一題程式碼審查。

## 技術組成

- Java 21、Spring Boot 3.5.16、Spring Batch 5、Spring Data JPA（Hibernate）
- 測試用 H2（記憶體），也可以選擇搭配 PostgreSQL
- Maven Wrapper（`./mvnw`），不需要另外安裝 Maven

## 快速開始

```bash
# 全部測試（約 20～30 秒）。一開始應該有 11 個練習測試失敗，其他測試全部通過。
./mvnw test

# 只跑某一題
./mvnw test -Dtest=Ex04LargeCobRunTest

# 啟動應用（H2 記憶體資料庫）
./mvnw spring-boot:run
```

### 使用 PostgreSQL（選用）

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

測試永遠使用 H2，不受這個設定影響。

## 練習方式

1. 依序閱讀 `exercises/NN-*/task.md`。每題只描述營運或 QA 回報的現象和重現方式。
2. 執行該題的測試，確認它失敗。
3. 找出根本原因並修正**產品程式碼**（`src/main`）。**不要修改測試**。
4. 該題測試通過，而且其他原本通過的測試仍然通過，就完成了。
5. 每題的 bug 彼此獨立，可以用任何順序做。建議每修好一題就 commit 一次。
6. 第 12 題是程式碼審查，沒有測試。

| # | 題目 | 測試 | 難度 |
|---|---|---|---|
| 01 | 被拒絕的還款出現在還款明細裡 | `Ex01RejectedRepaymentTest` | ★★☆☆☆ |
| 02 | 已婉拒的貸款申請仍然入帳 | `Ex02DeclinedLoanApplicationTest` | ★☆☆☆☆ |
| 03 | COB 處理失敗，但查不到失敗原因 | `Ex03CobFailureDiagnosticsTest` | ★★☆☆☆ |
| 04 | 貸款量一大，COB 就漏處理或重複處理 | `Ex04LargeCobRunTest` | ★★★★☆ |
| 05 | COB 期間分行入帳，該筆貸款當天沒有關帳 | `Ex05TellerDuringCobTest` | ★★★★☆ |
| 06 | 逾期報表越來越慢 | `Ex06OverdueReportTest` | ★☆☆☆☆ |
| 07 | 同一營業日重跑 COB，利息被計了兩次 | `Ex07CobRerunTest` | ★★☆☆☆ |
| 08 | COB 中止後，健康的貸款整天無法還款 | `Ex08AbortedCobTest` | ★★☆☆☆ |
| 09 | 連續兩晚的 COB，第二晚像是還停在第一晚 | `Ex09ConsecutiveBusinessDaysTest` | ★★★★☆ |
| 10 | 申請量瞬間湧入時，服務卡住幾秒後大量失敗 | `Ex10LoanApplicationBurstTest` | ★★★★☆ |
| 11 | 美元貸款的每日利息和對帳試算表差幾分錢 | `Ex11InterestReconciliationTest` | ★★☆☆☆ |
| 12 | 程式碼審查 | － | ★★★☆☆ |

## 領域模型

- **Loan（放款）**：本金、年利率、幣別（TWD／JPY 取整數，USD 取到分）、應計利息、應計罰息、狀態（ACTIVE／OVERDUE／CLOSED）、`last_closed_business_date`（最後一次被 COB 關帳的營業日）。
- **Installment（分期）**：等額本金月繳，最後一期吸收除不盡的餘數。到期日早於關帳日而且未繳清，就是逾期。
- **Business date（營業日）**：存在 `business_date` 表。API 請求和 COB 執行時，日期放在 thread-local 的 `BusinessDateContext` 中（類似 Fineract 的 `ThreadLocalContextUtil`）。
- **Account lock（帳戶鎖）**：COB 處理期間鎖住貸款，還款 API 會回 409。處理失敗的貸款，鎖會保留並記錄錯誤訊息。

## COB 批次流程

```
POST /api/cob/close-business-day
  └─ CobService：設定 BusinessDateContext（關帳日 = 目前營業日）
      └─ loanCobJob
          └─ loanCobManagerStep（partitioner：依 partition-size 切分要關帳的貸款 id）
              └─ loanCobWorkerStep × N（ThreadPoolTaskExecutor，本地分區）
                  ├─ beforeStep：替 partition 內的貸款上鎖（account_lock）
                  ├─ chunk：read → process（business steps）→ write（儲存＋解鎖）
                  │    business steps：資料完整性檢查 → 逾期判定 → 計息 → 逾期罰息
                  │    retry：樂觀鎖衝突；skip：其他例外（資料毀損除外）
                  ├─ 失敗時：錯誤訊息寫回鎖表
                  └─ afterStep：釋放 partition 內無錯誤的鎖
  └─ 成功後營業日 +1
```

主要設定在 `src/main/resources/application.yml` 的 `mini-cob.*`。

## REST API

| 方法 | 路徑 | 說明 |
|---|---|---|
| POST | `/api/loans` | 建立放款（`customerId`、`currency`、`principal`、`annualInterestRate`、`termMonths`） |
| GET | `/api/loans/{id}` | 查詢放款與攤還表 |
| POST | `/api/loans/{id}/repayments` | 還款，需要 `Idempotency-Key` 標頭，body：`{"amount": ...}` |
| GET | `/api/loans/{id}/repayments` | 還款明細 |
| GET | `/api/reports/overdue-loans` | 逾期報表 |
| GET | `/api/business-date` | 目前營業日 |
| POST | `/api/cob/close-business-day` | 關帳並推進營業日 |
| POST | `/api/cob/runs?cobDate=YYYY-MM-DD` | 重跑指定日期的 COB |

```bash
curl -s -X POST localhost:8080/api/loans -H 'Content-Type: application/json' \
  -d '{"customerId":"C-1","currency":"TWD","principal":120000,"annualInterestRate":0.073,"termMonths":3}'
curl -s -X POST localhost:8080/api/loans/1/repayments -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: pay-001' -d '{"amount":1000}'
curl -s -X POST localhost:8080/api/cob/close-business-day
```

## 測試輔助

- `IntegrationTestBase`：每個測試開始前清空資料，並把營業日重設為 2026-04-16。
- `TestLoans`：直接用領域模型建立貸款（不經過 API）。
- `TellerActivitySimulator`：只存在於測試中的 COB 步驟，用另一條連線模擬「分行在 COB 處理中入帳」。

## 參考

設計參考 Apache Fineract 的 `fineract-cob` 模組，以及 `fineract-provider` 中 `org.apache.fineract.cob.loan` 底下的 `LoanCOBManagerConfiguration`、`LoanCOBWorkerConfiguration`、`AccountLock`。本專案沒有複製 Fineract 的程式碼。
