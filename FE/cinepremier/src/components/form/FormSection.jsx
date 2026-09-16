import React from 'react';

export default function FormSection({ title, description, children, className = '' }) {
  return (
    <section className={`border border-white/10 bg-black p-5 ${className}`}>
      {(title || description) && (
        <div className="mb-5 border-b border-white/10 pb-4">
          {title && <h3 className="text-sm font-black uppercase tracking-[0.18em] text-white">{title}</h3>}
          {description && <p className="mt-2 text-xs leading-6 text-neutral-500">{description}</p>}
        </div>
      )}
      <div className="space-y-4">{children}</div>
    </section>
  );
}
