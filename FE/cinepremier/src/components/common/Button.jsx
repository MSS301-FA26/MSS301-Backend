import React from 'react';

const variants = {
  primary: 'border-white bg-white text-black hover:bg-black hover:text-white',
  secondary: 'border-white/15 bg-black text-white hover:border-white',
  danger: 'border-rose-500/40 bg-rose-950/30 text-rose-200 hover:bg-rose-500 hover:text-white',
  ghost: 'border-transparent bg-transparent text-neutral-300 hover:bg-white/10 hover:text-white',
};

export default function Button({
  as: Component = 'button',
  type = 'button',
  variant = 'primary',
  className = '',
  children,
  ...props
}) {
  return (
    <Component
      type={Component === 'button' ? type : undefined}
      className={`inline-flex items-center justify-center gap-2 border px-4 py-2 text-[10px] font-black uppercase tracking-[0.18em] transition disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant] || variants.primary} ${className}`}
      {...props}
    >
      {children}
    </Component>
  );
}
