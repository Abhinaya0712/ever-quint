
function parseHeights(value) {
  return value
    .split(',')
    .map(Number)
    .filter(height => Number.isFinite(height) && height >= 0);
}

function trappedWater(heights) {
  const leftMax = new Array(heights.length).fill(0);
  const rightMax = new Array(heights.length).fill(0);
  const water = new Array(heights.length).fill(0);

  let maximum = 0;
  for (let i = 0; i < heights.length; i++) {
    leftMax[i] = maximum;
    maximum = Math.max(maximum, heights[i]);
  }

  maximum = 0;
  for (let i = heights.length - 1; i >= 0; i--) {
    rightMax[i] = maximum;
    maximum = Math.max(maximum, heights[i]);
  }

  let total = 0;
  for (let i = 0; i < heights.length; i++) {
    water[i] = Math.max(0, Math.min(leftMax[i], rightMax[i]) - heights[i]);
    total += water[i];
  }

  return { total, water };
}

function createSVG(heights, waterLevels = null) {
  const width = 680;
  const cellWidth = 58;
  const cellHeight = 39;
  const rows = Math.max(...heights, 6);
  const height = cellHeight * (rows + 1);
  const left = 4;

  let svg = `<svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Water tank chart">
    <g stroke="#9da3a8" stroke-width="1">`;

  for (let row = 0; row <= rows; row++) {
    const y = row * cellHeight;
    svg += `<line x1="${left}" y1="${y}" x2="${left + cellWidth * heights.length}" y2="${y}"/>`;
  }

  for (let column = 0; column <= heights.length; column++) {
    const x = left + column * cellWidth;
    svg += `<line x1="${x}" y1="0" x2="${x}" y2="${rows * cellHeight}"/>`;
  }

  svg += `</g>`;

  heights.forEach((blockHeight, column) => {
    const x = left + column * cellWidth;

    if (waterLevels) {
      for (let level = 0; level < waterLevels[column]; level++) {
        const y = (rows - blockHeight - level - 1) * cellHeight;
        svg += `<rect x="${x + 1}" y="${y + 1}" width="${cellWidth - 2}" height="${cellHeight - 2}" fill="#4d78a8"/>`;
      }
    }

    for (let level = 0; level < blockHeight; level++) {
      const y = (rows - level - 1) * cellHeight;
      svg += `<rect x="${x + 1}" y="${y + 1}" width="${cellWidth - 2}" height="${cellHeight - 2}" fill="#ffd900"/>`;
    }
  });

  svg += `</svg>`;
  return svg;
}

function calculate() {
  const heights = parseHeights(document.getElementById('heightInput').value);

  if (!heights.length) {
    document.getElementById('result').innerHTML = 'Output: <strong>Invalid input</strong>';
    document.getElementById('tankView').innerHTML = '';
    document.getElementById('waterView').innerHTML = '';
    return;
  }

  const solution = trappedWater(heights);
  document.getElementById('exampleInput').textContent = `[${heights.join(',')}]`;
  document.getElementById('tankView').innerHTML = createSVG(heights);
  document.getElementById('waterView').innerHTML = createSVG(heights, solution.water);
  document.getElementById('result').innerHTML = `Output: <strong>${solution.total} Units</strong>`;
}

const heights = [0, 4, 0, 0, 0, 6, 0, 6, 4, 0];
document.getElementById('tankView').innerHTML = createSVG(heights);
const initial = trappedWater(heights);
document.getElementById('waterView').innerHTML = createSVG(heights, initial.water);
document.getElementById('calculateButton').addEventListener('click', calculate);
document.getElementById('heightInput').addEventListener('keydown', event => {
  if (event.key === 'Enter') calculate();
});
