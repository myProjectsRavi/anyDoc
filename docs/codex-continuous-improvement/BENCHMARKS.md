# Benchmarks

## Baseline status — Cycle 001

No new performance percentage is claimed in this run.

The local sandbox could not clone the repository because its network layer could not resolve `github.com`. The connected GitHub repository is available for source inspection and mutation, but that does not provide a trustworthy runtime benchmark environment by itself.

### Metrics requiring baselines
| Area | Metric | Baseline | Status |
|---|---|---:|---|
| App startup | cold/warm startup p50/p95 | not measured | instrumentation required |
| PDF compression | ms/page, p50/p95, peak bitmap memory | not measured | benchmark required |
| PDF merge/split | throughput + peak heap/temp disk | not measured | benchmark required |
| Scanner | edge detection/frame + perspective correction | not measured | benchmark required |
| OCR | ms/page + peak heap | not measured | benchmark required |
| Image conversion | images/min + peak heap | not measured | benchmark required |
| Audio conversion | realtime factor + temp disk | not measured | benchmark required |
| Batch queue | throughput + cancellation latency | not measured | benchmark required |
| Compose | recompositions/jank | not measured | macrobenchmark/UI instrumentation required |

### Measurement policy
For each future performance change:
1. freeze representative input;
2. run multiple baseline iterations;
3. record device/emulator/runner details;
4. make one focused change;
5. repeat the same test;
6. report median/p95 where meaningful and variance/limitations;
7. calculate percentage only from measured values.

Until then use: **5% target not safely measurable yet**.
