import { UseFormSetError, FieldValues, Path } from 'react-hook-form';

export interface BackendError extends Error {
  code?: string;
  status?: number;
  fieldErrors?: { field: string; code: string; message: string }[];
}

/**
 * Maps backend fieldErrors to react-hook-form inputs.
 * @param error The wrapped error rejected from apiClient interceptor.
 * @param setError The react-hook-form setError function.
 * @returns true if there were field errors mapped, false otherwise.
 */
export function setServerErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>
): boolean {
  if (error instanceof Error) {
    const backendError = error as BackendError;
    if (backendError.fieldErrors && Array.isArray(backendError.fieldErrors) && backendError.fieldErrors.length > 0) {
      backendError.fieldErrors.forEach((fe) => {
        setError(fe.field as Path<T>, {
          type: fe.code || 'server',
          message: fe.message,
        });
      });
      return true;
    }
  }
  return false;
}
