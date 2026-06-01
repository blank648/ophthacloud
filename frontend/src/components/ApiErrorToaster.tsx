import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import type { ApiFieldError } from '@/lib/apiClient';

type AppError = Error & {
  code?: string;
  status?: number;
  fieldErrors?: ApiFieldError[];
};

/**
 * Subscribes to React Query cache events and surfaces standardized
 * API errors as toasts, per backend ApiResponse contract:
 *  - 400 VALIDATION_ERROR  → toast + field errors are available on the error
 *  - 403                   → "Nu ai permisiunea pentru această acțiune"
 *  - 409 DOUBLE_BOOKING    → conflict toast
 *  - 409 CONSULTATION_ALREADY_SIGNED → readonly notice
 *  - default               → generic error toast with code + message
 */
export const ApiErrorToaster: React.FC = () => {
  const qc = useQueryClient();

  useEffect(() => {
    const cache = qc.getQueryCache();
    const mcache = qc.getMutationCache();

    const handleError = (rawError: unknown) => {
      const err = rawError as AppError;
      if (!err) return;

      const code = err.code ?? 'UNKNOWN_ERROR';
      const status = err.status;

      if (status === 403) {
        toast.error('Nu ai permisiunea pentru această acțiune');
        return;
      }

      if (code === 'DOUBLE_BOOKING') {
        toast.error('Conflict programare', {
          description: err.message || 'Există deja o programare în acest interval.',
        });
        return;
      }

      if (code === 'CONSULTATION_ALREADY_SIGNED') {
        toast.warning('Consultație semnată — doar vizualizare', {
          description: err.message,
        });
        return;
      }

      if (code === 'VALIDATION_ERROR' || status === 400) {
        const first = err.fieldErrors?.[0];
        toast.error('Date invalide', {
          description: first ? `${first.field}: ${first.message}` : err.message,
        });
        return;
      }

      // Don't double-toast 401 (handled by interceptor refresh) when silent
      if (status === 401) return;

      toast.error(err.message || 'Eroare API', {
        description: code !== err.message ? code : undefined,
      });
    };

    const unsubQ = cache.subscribe((event) => {
      if (event.type === 'updated' && event.action.type === 'error') {
        handleError(event.action.error);
      }
    });

    const unsubM = mcache.subscribe((event) => {
      if (event.type === 'updated' && event.action.type === 'error') {
        handleError(event.action.error);
      }
    });

    return () => {
      unsubQ();
      unsubM();
    };
  }, [qc]);

  return null;
};
