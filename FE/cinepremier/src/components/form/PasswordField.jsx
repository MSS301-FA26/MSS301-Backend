import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import TextInput from './TextInput';

export default function PasswordField({ inputClassName = '', ...props }) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="relative">
      <TextInput
        type={visible ? 'text' : 'password'}
        inputClassName={`pr-11 ${inputClassName}`}
        {...props}
      />
      <button
        type="button"
        onClick={() => setVisible((value) => !value)}
        className="absolute right-4 top-[2.45rem] text-neutral-500 transition hover:text-white"
        aria-label={visible ? 'An mat khau' : 'Hien mat khau'}
      >
        {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
      </button>
    </div>
  );
}
