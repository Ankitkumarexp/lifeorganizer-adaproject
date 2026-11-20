# ADA Algorithm Analysis

| Algorithm | Best time | Average time | Worst time | Auxiliary space |
|---|---:|---:|---:|---:|
| Linear search | O(1) | O(n) | O(n) | O(1) |
| Binary search | O(1) | O(log n) | O(log n) | O(1), iterative |
| Bubble sort (early exit) | O(n) | O(n²) | O(n²) | O(1) |
| Selection sort | O(n²) | O(n²) | O(n²) | O(1) |
| Insertion sort | O(n) | O(n²) | O(n²) | O(1) |
| Merge sort | O(n log n) | O(n log n) | O(n log n) | O(n) |
| Quick sort | O(n log n) | O(n log n) | O(n²) | O(log n) average recursion |
| Binary heap priority queue | O(1) peek | O(log n) insert/remove | O(log n) insert/remove | O(n) |

Binary search requires sorted input. The Algorithm Lab sorts input before binary search. Runtime measurements depend on hardware, JVM warm-up and input size. Do not use a single tiny sample as proof of performance superiority.
