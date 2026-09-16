import React from 'react';
import { X } from 'lucide-react';
import Button from './Button';

export default function Modal({ open, title, children, onClose, className = '' }) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 px-4 backdrop-blur-sm">
      <section className={`relative w-full max-w-lg border border-white/10 bg-black p-6 text-white shadow-2xl ${className}`}>
        <div className="mb-5 flex items-center justify-between gap-4 border-b border-white/10 pb-4">
          {title && <h2 className="font-serif text-lg font-black italic uppercase tracking-wider">{title}</h2>}
          <Button variant="ghost" className="ml-auto h-9 w-9 p-0" onClick={onClose} aria-label="Dong modal">
            <X className="h-4 w-4" />
          </Button>
        </div>
        {children}
      </section>
    </div>
  );
}
