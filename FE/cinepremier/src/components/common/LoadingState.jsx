import React from 'react';
import { Loader2 } from 'lucide-react';

export default function LoadingState({ label = 'Dang tai du lieu...' }) {
  return (
    <div className="flex items-center justify-center gap-3 border border-white/10 bg-black px-5 py-8 text-xs font-black uppercase tracking-widest text-neutral-400">
      <Loader2 className="h-4 w-4 animate-spin text-amber-400" />
      {label}
    </div>
  );
}
