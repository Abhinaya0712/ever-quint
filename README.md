# Ever Quint — Water Tank Problem

A responsive vanilla JavaScript implementation of the trapping-rain-water problem.

## Run locally

Open `index.html` directly in a browser, or serve the directory:

```bash
python3 -m http.server 8000
```

Then visit `http://localhost:8000`.

## Algorithm

The app computes trapped water with prefix and suffix maximum arrays:

- **Time:** `O(n)`
- **Space:** `O(n)`

Enter comma-separated non-negative heights in the input field and select **Calculate**.
