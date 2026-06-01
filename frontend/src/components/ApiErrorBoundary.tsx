import React from 'react';
import { toast } from 'sonner';

interface Props {
  children: React.ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ApiErrorBoundary extends React.Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error) {
    const code = (error as Error & { code?: string }).code;
    toast.error('A apărut o eroare', {
      description: code ? `${code} — ${error.message}` : error.message,
    });
    console.error('ApiErrorBoundary caught:', error);
  }

  reset = () => this.setState({ hasError: false, error: undefined });

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-background p-6">
          <div className="max-w-md w-full bg-card border border-border rounded-xl shadow-sm p-6 text-center">
            <h2 className="text-clinical-lg font-bold text-foreground mb-2">
              Ceva nu a mers bine
            </h2>
            <p className="text-clinical-sm text-muted-foreground mb-4">
              {this.state.error?.message || 'Eroare necunoscută'}
            </p>
            <button
              onClick={this.reset}
              className="px-4 py-2 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold hover:opacity-90"
            >
              Reîncearcă
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
