'use client';

import React, { useState, useRef, useEffect, KeyboardEvent } from 'react';
import { transliterateGujlish, getGujlishSuggestions } from '@/lib/transliterate';
import { Loader2 } from 'lucide-react';

interface GujlishInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  value: string;
  onValueChange: (value: string) => void;
  label?: string;
  helperText?: string;
}

export function GujlishInput({
  value,
  onValueChange,
  label,
  helperText,
  className = '',
  placeholder = 'અહીં ટાઇપ કરો (દા.ત. namaste gujarati લખીને Space દબાવો)...',
  disabled,
  required,
  ...rest
}: GujlishInputProps) {
  const [isConverting, setIsConverting] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [targetWordMeta, setTargetWordMeta] = useState<{ word: string; start: number; end: number } | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Transliterate word when Space or Enter is pressed (exact match to indiatyping.com)
  async function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (disabled) return;

    if (e.key === ' ' || e.key === 'Enter') {
      const input = inputRef.current;
      if (!input) return;

      const cursorPosition = input.selectionStart ?? value.length;
      const textBeforeCursor = value.slice(0, cursorPosition);
      const textAfterCursor = value.slice(cursorPosition);

      // Check if there is an English/phonetic word right before cursor
      const lastWordMatch = textBeforeCursor.match(/([a-zA-Z0-9]+)$/);
      if (lastWordMatch && lastWordMatch[1]) {
        const lastWord = lastWordMatch[1];
        const matchIndex = lastWordMatch.index ?? (textBeforeCursor.length - lastWord.length);

        e.preventDefault();
        setIsConverting(true);
        setShowSuggestions(false);

        try {
          const converted = await transliterateGujlish(lastWord);
          const prefix = textBeforeCursor.slice(0, matchIndex);
          const separator = e.key === 'Enter' ? '\n' : ' ';
          const newText = prefix + converted + separator + textAfterCursor;

          onValueChange(newText);

          const newCursorPos = prefix.length + converted.length + separator.length;
          requestAnimationFrame(() => {
            if (inputRef.current) {
              inputRef.current.focus();
              inputRef.current.setSelectionRange(newCursorPos, newCursorPos);
            }
          });
        } catch {
          onValueChange(textBeforeCursor + (e.key === 'Enter' ? '\n' : ' ') + textAfterCursor);
        } finally {
          setIsConverting(false);
        }
      }
    }
  }

  return (
    <div className="w-full space-y-1">
      {label && (
        <div className="flex items-center justify-between text-xs">
          <label className="font-semibold text-slate-800">
            {label} {required && <span className="text-rose-500">*</span>}
          </label>
          {isConverting && (
            <span className="flex items-center gap-1 text-[11px] text-amber-600 font-medium">
              <Loader2 className="w-3 h-3 animate-spin" /> કન્વર્ટ થાય છે...
            </span>
          )}
        </div>
      )}

      <div className="relative">
        <input
          ref={inputRef}
          value={value}
          onChange={(e) => onValueChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled}
          required={required}
          className={`w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-navy-600 focus:outline-none focus:ring-2 focus:ring-navy-600/20 transition-all ${className}`}
          {...rest}
        />
      </div>

      <div className="flex items-center justify-between text-[11px] text-slate-400 px-0.5">
        <span>{helperText || 'અંગ્રેજી / ગુજલિશમાં લખી Space આપતાં જ ગુજરાતી થઈ જશે (દા.ત. "kem cho" -> "કેમ છો")'}</span>
      </div>
    </div>
  );
}

interface GujlishTextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  value: string;
  onValueChange: (value: string) => void;
  label?: string;
  helperText?: string;
  rows?: number;
}

export function GujlishTextarea({
  value,
  onValueChange,
  label,
  helperText,
  className = '',
  placeholder = 'અહીં સંદેશ ટાઇપ કરો (દા.ત. badha aavjo લખીને Space દબાવો)...',
  disabled,
  rows = 4,
  required,
  ...rest
}: GujlishTextareaProps) {
  const [isConverting, setIsConverting] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  async function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (disabled) return;

    if (e.key === ' ' || e.key === 'Enter') {
      const textarea = textareaRef.current;
      if (!textarea) return;

      const cursorPosition = textarea.selectionStart ?? value.length;
      const textBeforeCursor = value.slice(0, cursorPosition);
      const textAfterCursor = value.slice(cursorPosition);

      const lastWordMatch = textBeforeCursor.match(/([a-zA-Z0-9]+)$/);
      if (lastWordMatch && lastWordMatch[1]) {
        const lastWord = lastWordMatch[1];
        const matchIndex = lastWordMatch.index ?? (textBeforeCursor.length - lastWord.length);

        e.preventDefault();
        setIsConverting(true);

        try {
          const converted = await transliterateGujlish(lastWord);
          const prefix = textBeforeCursor.slice(0, matchIndex);
          const separator = e.key === 'Enter' ? '\n' : ' ';
          const newText = prefix + converted + separator + textAfterCursor;

          onValueChange(newText);

          const newCursorPos = prefix.length + converted.length + separator.length;
          requestAnimationFrame(() => {
            if (textareaRef.current) {
              textareaRef.current.focus();
              textareaRef.current.setSelectionRange(newCursorPos, newCursorPos);
            }
          });
        } catch {
          onValueChange(textBeforeCursor + (e.key === 'Enter' ? '\n' : ' ') + textAfterCursor);
        } finally {
          setIsConverting(false);
        }
      }
    }
  }

  return (
    <div className="w-full space-y-1">
      {label && (
        <div className="flex items-center justify-between text-xs">
          <label className="font-semibold text-slate-800">
            {label} {required && <span className="text-rose-500">*</span>}
          </label>
          {isConverting && (
            <span className="flex items-center gap-1 text-[11px] text-amber-600 font-medium">
              <Loader2 className="w-3 h-3 animate-spin" /> કન્વર્ટ થાય છે...
            </span>
          )}
        </div>
      )}

      <textarea
        ref={textareaRef}
        rows={rows}
        value={value}
        onChange={(e) => onValueChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={disabled}
        required={required}
        className={`w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-navy-600 focus:outline-none focus:ring-2 focus:ring-navy-600/20 transition-all ${className}`}
        {...rest}
      />

      <div className="flex items-center justify-between text-[11px] text-slate-400 px-0.5">
        <span>{helperText || 'અંગ્રેજી / ગુજલિશમાં લખી Space આપતાં જ ગુજરાતી થઈ જશે'}</span>
      </div>
    </div>
  );
}
