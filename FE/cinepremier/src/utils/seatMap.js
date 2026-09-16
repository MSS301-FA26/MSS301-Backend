// Generates dynamic seat maps based on pricing and availability rules.
export function generateSeats() {
  const list = [];
  const rows = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L'];
  const cols = 14;

  rows.forEach((row) => {
    const isCoupleRow = row === 'L';
    const isStandardRow = ['A', 'B', 'C'].includes(row);
    const isVipRow = !isCoupleRow && !isStandardRow;

    for (let c = 1; c <= cols; c++) {
      let type = 'standard';
      let price = 95000;

      if (isVipRow) {
        type = 'vip';
        price = 145000;
      } else if (isCoupleRow) {
        type = 'couple';
        price = 280000;
      }

      const isMiddle = c >= 4 && c <= 11;
      const seed = Math.random();
      const isBooked = (isMiddle && seed < 0.45) || (!isMiddle && seed < 0.15);

      list.push({
        id: `${row}${c}`,
        row,
        col: c,
        type,
        price,
        isBooked,
      });
    }
  });

  return list;
}
