# Velocity Lens (Java)

A small Java planner that picks this sprint's work from your backlog. Each task gets a value from its impact, urgency and confidence, and the planner packs the sprint for the most total value that fits the effort you have.

## Run it on your backlog

Export your backlog to CSV with these columns, in any order:

```
name,effort,impact,urgency,confidence
"Fix login timeout, mobile",3,9,9,90%
Flaky test sweep,2,4,6,95
"Rewrite ""Pricing"" page",4,7,5,0.8
```

```powershell
javac -encoding UTF-8 -d out src\main\java\com\velocitylens\velocity\*.java tests\*.java
java -cp out com.velocitylens.velocity.Main examples\backlog.csv --budget 10
```

```
Velocity Lens sprint batch (budget 10.00, most value):
- Fix login timeout, mobile (effort=3.00, value=24.30, score=8.100)
- Flaky test sweep (effort=2.00, value=13.87, score=6.935)
- Rewrite "Pricing" page (effort=4.00, value=14.72, score=3.680)
Value 52.89, effort 9.00 of 10.00.
  left out: Export to CSV (the rest is worth more together)
```

With no file it plans a three-task sample. `--greedy` takes tasks in score order instead, for comparison.

The CSV reader follows RFC 4180, so a sheet saved from Excel or Google Sheets works as is: quoted fields with commas or line breaks, doubled quotes, CRLF line ends and the byte order mark Excel adds. Columns are matched by header name. A bad cell stops the run with the row and column named (`row 3: urgency "x" is not a number`).

## How the sprint is packed

A task's value is `(1.7 x impact + 1.3 x urgency) x confidence`, and its score is value divided by effort. Effort is charged at least 0.25, so tiny tasks can't fill the sprint for free.

Taking tasks in score order until the budget runs out is Dantzig's greedy rule for the knapsack problem, and it can leave value on the table. With a budget of 10, a 6-effort task worth 7 has the best score, but two 5-effort tasks worth 5.5 each are worth 11 together. On the built-in sample with a budget of 6.5, score order picks the copy pass and the telemetry cleanup; the packer swaps the cleanup for the portfolio refresh and the sprint is worth more.

`Planner.buildSprint` solves the 0/1 knapsack exactly by dynamic programming over hundredths of effort. Efforts round up, so a plan never runs over. The tests check it against brute force on 300 random backlogs and check it never does worse than score order. Above two million table cells it falls back to score order.

Inputs are checked before scoring: NaN, infinite and negative numbers are rejected with the task named, and confidence can be a fraction (`0.8`) or a percent (`80`, `80%`), the way RICE writes it.

## Test

```powershell
java -ea -cp out TestPlanner
```

CI runs the tests on Java 17 and 21.
