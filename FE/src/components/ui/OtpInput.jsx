import {
  useCallback,
  useEffect,
  useId,
  useImperativeHandle,
  useRef,
  useState,
} from 'react';

function digitsOnly(value, length) {
  return String(value ?? '').replace(/\D/g, '').slice(0, length);
}

export function OtpInput({
  length = 6,
  defaultValue = '',
  onChange,
  onComplete,
  status = 'idle',
  errorMessage = '',
  successMessage = '',
  hint = '',
  label = 'Verification code',
  disabled = false,
  autoFocus = false,
  focusOnError = true,
  className = '',
  ref,
}) {
  const [value, setValue] = useState(() => digitsOnly(defaultValue, length));
  const inputRef = useRef(null);
  const statusId = useId();
  const error = status === 'error';
  const success = status === 'success';

  const focus = useCallback(() => inputRef.current?.focus(), []);
  const commit = useCallback((nextValue) => {
    const next = digitsOnly(nextValue, length);
    setValue(next);
    onChange?.(next);
    if (next.length === length) onComplete?.(next);
  }, [length, onChange, onComplete]);
  const clear = useCallback(() => {
    commit('');
    focus();
  }, [commit, focus]);

  useImperativeHandle(ref, () => ({ clear, focus }), [clear, focus]);

  useEffect(() => {
    if (error && focusOnError && !disabled) focus();
  }, [disabled, error, focus, focusOnError]);

  const hasStatus = Boolean(hint || errorMessage || successMessage);
  const message = error ? errorMessage : success ? successMessage : hint;
  const messageTone = error
    ? 'text-rose-600 dark:text-rose-400'
    : success
      ? 'text-emerald-600 dark:text-emerald-400'
      : 'text-slate-500 dark:text-slate-400';

  return (
    <div className={`inline-flex w-64 flex-col ${className}`}>
      <input
        ref={inputRef}
        type="text"
        value={value}
        maxLength={length}
        inputMode="numeric"
        pattern="[0-9]*"
        autoComplete="one-time-code"
        autoCorrect="off"
        autoCapitalize="off"
        spellCheck={false}
        autoFocus={autoFocus}
        disabled={disabled}
        aria-label={label}
        aria-invalid={error || undefined}
        aria-describedby={hasStatus ? statusId : undefined}
        onChange={(event) => commit(event.currentTarget.value)}
        onPaste={(event) => {
          const pasted = event.clipboardData.getData('text');
          const digits = digitsOnly(pasted, length);
          if (digits !== pasted || digits.length === length) {
            event.preventDefault();
            commit(digits);
          }
        }}
        className={`h-12 w-full rounded-[10px] border-2 bg-white px-4 pl-[0.5em] text-center font-mono text-[15px] tracking-[0.5em] tabular-nums text-slate-700 outline-none transition-[background-color,border-color,box-shadow] duration-150 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-zinc-900 dark:text-slate-200 dark:focus:border-indigo-400 ${
          error
            ? 'border-rose-500 dark:border-rose-400'
            : success
              ? 'border-emerald-500 dark:border-emerald-400'
              : 'border-slate-300 dark:border-slate-600'
        }`}
      />

      {hasStatus && (
        <p
          id={statusId}
          role="status"
          className={`mt-2 h-4 text-[11.5px] leading-[16px] ${messageTone}`}
        >
          {message}
        </p>
      )}
    </div>
  );
}

export default OtpInput;
