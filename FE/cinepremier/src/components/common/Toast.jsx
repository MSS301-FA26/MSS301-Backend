import React from 'react';
import { X } from 'lucide-react';

export default function Toast({ toast, onClose }) {
  if (!toast) return null;

  const isSad = toast.tone === 'sad';
  const hasAction = toast.action?.onClick && toast.action?.label;

  return (
    <div className="fixed top-6 right-6 z-[120] max-w-sm w-full">
      <div className={`border p-4 text-white rounded shadow-xl ${
        isSad
          ? 'border-red-400/50 bg-red-950'
          : 'border-green-400/50 bg-green-950'
      }`}>
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3 flex-1 min-w-0">
            <span className={`text-sm font-bold shrink-0 ${
              isSad ? 'text-red-200' : 'text-green-200'
            }`}>
              {isSad ? '✗' : '✓'}
            </span>
            <p className="text-sm leading-relaxed whitespace-pre-line">{toast.text}</p>
          </div>
          <button
            onClick={onClose}
            className="shrink-0 text-neutral-400 hover:text-white transition"
            aria-label="Đóng"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Action + Cancel buttons */}
        {hasAction && (
          <div className="flex items-center gap-2 mt-3 pt-3 border-t border-white/10">
            <button
              onClick={() => {
                toast.action.onClick();
                onClose();
              }}
              className={`flex-1 py-1.5 px-3 text-xs font-black uppercase tracking-wider rounded transition ${
                isSad
                  ? 'bg-red-500 hover:bg-red-400 text-white'
                  : 'bg-green-500 hover:bg-green-400 text-white'
              }`}
            >
              {toast.action.label}
            </button>
            <button
              onClick={onClose}
              className="flex-1 py-1.5 px-3 text-xs font-black uppercase tracking-wider rounded border border-white/20 text-neutral-300 hover:text-white hover:border-white/50 transition"
            >
              Hủy
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
