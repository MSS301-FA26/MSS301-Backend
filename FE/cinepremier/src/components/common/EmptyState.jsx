import React from 'react';
import { Film } from 'lucide-react';
import Button from './Button';

export default function EmptyState({ icon: Icon = Film, title, description, actionLabel, onAction }) {
  return (
    <div className="border border-dashed border-white/10 bg-black px-6 py-12 text-center">
      <Icon className="mx-auto h-9 w-9 text-neutral-600" />
      {title && <h2 className="mt-5 font-serif text-xl italic tracking-wider text-white">{title}</h2>}
      {description && <p className="mx-auto mt-3 max-w-md text-xs leading-6 text-neutral-500">{description}</p>}
      {actionLabel && (
        <Button className="mt-6" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
}
