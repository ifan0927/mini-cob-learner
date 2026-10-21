# 06｜逾期報表越來越慢

- 難度：★☆☆☆☆
- 回報單位：DBA

## 現象

催收部門每天早上會呼叫好幾次逾期報表 API（`GET /api/reports/overdue-loans`）。隨著逾期件數增加，這支 API 越來越慢。

DBA 從資料庫的統計看到，每呼叫一次，執行的 SQL 數量就和逾期貸款筆數差不多。逾期一萬筆，就會有一萬多條 SQL。

## 重現方式

1. 建立 20 筆已逾期的貸款。
2. 呼叫一次 `GET /api/reports/overdue-loans`，計算這次請求執行了幾條 SQL。

## 期望行為

報表內容不變（20 筆），但一次呼叫執行的 SQL 數量不能隨貸款筆數成長。在這個情境下最多 5 條。

## 驗證

```bash
./mvnw test -Dtest=Ex06OverdueReportTest
```
