import React from 'react';

export default function Table({ columns = [], rows = [], getRowKey = (_, index) => index, emptyText = 'Khong co du lieu' }) {
  return (
    <div className="overflow-x-auto border border-white/10 bg-black">
      <table className="min-w-full divide-y divide-white/10 text-left text-xs">
        <thead className="bg-neutral-950 text-[9px] font-black uppercase tracking-[0.18em] text-neutral-500">
          <tr>
            {columns.map((column) => (
              <th key={column.key} className="px-4 py-3">{column.header}</th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-white/5 text-neutral-300">
          {rows.length === 0 ? (
            <tr>
              <td className="px-4 py-8 text-center text-neutral-500" colSpan={Math.max(columns.length, 1)}>
                {emptyText}
              </td>
            </tr>
          ) : rows.map((row, index) => (
            <tr key={getRowKey(row, index)} className="transition hover:bg-white/5">
              {columns.map((column) => (
                <td key={column.key} className="px-4 py-3">
                  {column.render ? column.render(row, index) : row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
