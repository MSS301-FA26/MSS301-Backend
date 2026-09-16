import React from 'react';

export default function TextInput({ label, hint, error, className = '', inputClassName = '', ...props }) {
  return (
    <label className={`block space-y-2 ${className}`}>
      {label && <span className="block text-[10px] font-black uppercase tracking-[0.18em] text-neutral-500">{label}</span>}
      <input
        className={`w-full border border-white/15 bg-black px-4 py-3 text-sm text-white outline-none transition placeholder:text-neutral-700 focus:border-white ${inputClassName}`}
        {...props}
      />
      {hint && !error && <span className="block text-[10px] leading-5 text-neutral-500">{hint}</span>}
      {error && <span className="block text-[10px] leading-5 text-rose-300">{error}</span>}
    </label>
  );
}
