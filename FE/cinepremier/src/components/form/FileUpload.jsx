import React from 'react';
import { Upload } from 'lucide-react';

export default function FileUpload({ label = 'Chon tep', accept, onChange, disabled = false, className = '' }) {
  return (
    <label className={`inline-flex cursor-pointer items-center justify-center gap-2 border border-white/15 bg-black px-4 py-3 text-[10px] font-black uppercase tracking-[0.18em] text-white transition hover:border-white disabled:opacity-50 ${className}`}>
      <Upload className="h-4 w-4" />
      {label}
      <input
        type="file"
        accept={accept}
        disabled={disabled}
        onChange={onChange}
        className="sr-only"
      />
    </label>
  );
}
