# DUPLICATE_CHECK_FIX.md

## 🚨 Problem
Three bills for **October 2025** were causing confusion because they all belong to the same billing cycle:
1.  **Bill A:** 20,571.95 (Regular)
2.  **Bill B:** 20,571.95 (Storno)
3.  **Bill C:** 29,435.68 (Corrected)

The system previously treated them as three valid, separate bills because their Invoice Numbers were different.

## ✅ The Fix: "The Slot System"

We introduced a logic that groups bills by **Naplatni Broj (House)** + **Period**. Only one bill per "Slot" is allowed to be visible.

### 📜 The Rules (Hierarchy)

1.  **👑 Correction Rule:** If a bill is marked **KORIGOVAN**, it is the *only* valid bill. All others (Regular/Storno) are hidden.
    *   *Result:* Bill C becomes the only visible bill.

2.  **⚔️ Storno Rule:** If a Storno bill exists, it "kills" the corresponding Regular bill (same amount).
    *   *Result:* Bill B cancels Bill A.

3.  **⚖️ Tie-Breaker:** If multiple regular bills exist, the one with the highest Invoice Number wins.

## 🛠️ Implementation Details

*   **`EpsParser`:** Now explicitly logs `BILL TYPE: KORIGOVAN`.
*   **`BillDuplicateDetector`:** Added `resolveConflicts(naplatniBroj, period)` function.
*   **`ReceiptRepository`:** Automatically runs `resolveConflicts` after every new insertion.
*   **`EpsData`:** ID generation relies on Invoice Number + Period + Amount (ensuring unique IDs for all 3 files so they can exist in the DB, even if hidden).

## 🧪 Expected Behavior
When you scan these three bills:
1.  All 3 will be saved to the database (preserving history).
2.  **Only the Corrected Bill (29k)** will be visible in the list.
3.  The Storno and Regular bills (20k) will be automatically hidden (`isVisible = false`).