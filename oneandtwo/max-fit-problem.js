
function maxProfit(n) {

    // Building details: Name, Construction Time, Earnings
    const buildings = [
        { name: "T", time: 5, earning: 1500 },
        { name: "P", time: 4, earning: 1000 },
        { name: "C", time: 10, earning: 2000 }
    ];

    // DP array to store maximum earnings
    const dp = new Array(n + 1).fill(-Infinity);

    // Store all optimal combinations at each time
    const ways = Array.from(
        { length: n + 1 },
        () => new Set()
    );

    // Initial state
    dp[0] = 0;
    ways[0].add("0,0,0");

    // Dynamic Programming
    for (let t = 0; t <= n; t++) {

        if (dp[t] === -Infinity) {
            continue;
        }

        for (let i = 0; i < buildings.length; i++) {

            const building = buildings[i];

            const completionTime = t + building.time;

            // Check construction time limit
            if (completionTime > n) {
                continue;
            }

            // Calculate additional earnings
            const profit =
                dp[t] +
                building.earning * (n - completionTime);

            // Generate new combinations
            const newSolutions = new Set();

            for (const combination of ways[t]) {

                const counts = combination.split(",").map(Number);

                // Update building count
                counts[i]++;

                newSolutions.add(counts.join(","));
            }

            // Update maximum earnings
            if (profit > dp[completionTime]) {

                dp[completionTime] = profit;

                ways[completionTime].clear();

                for (const solution of newSolutions) {
                    ways[completionTime].add(solution);
                }

            } else if (profit === dp[completionTime]) {

                for (const solution of newSolutions) {
                    ways[completionTime].add(solution);
                }
            }
        }
    }

    // Find maximum earnings
    const maxEarnings = Math.max(...dp);

    console.log("Maximum Earnings: $" + maxEarnings);
    console.log("Solutions:");

    let count = 1;

    // Print all optimal combinations
    for (let t = 0; t <= n; t++) {

        if (dp[t] === maxEarnings) {

            const sortedSolutions = [...ways[t]].sort();

            for (const solution of sortedSolutions) {

                const [T, P, C] = solution.split(",");

                console.log(
                    `${count++}. T: ${T} P: ${P} C: ${C}`
                );
            }
        }
    }
}

// Test cases
console.log("Test Case 1:");
maxProfit(7);

console.log("\nTest Case 2:");
maxProfit(8);

console.log("\nTest Case 3:");
maxProfit(13);
