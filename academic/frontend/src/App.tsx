import { useEffect, useState } from 'react';
import { CheckCircle2, Eye, Contrast, Type } from 'lucide-react';
import authMock from './mocks/auth.mock.json';
import receptionMock from './mocks/reception.mock.json';
import clinicalMock from './mocks/clinical.mock.json';
import adherenceMock from './mocks/adherence.mock.json';
import errorsMock from './mocks/errors.mock.json';
import { USE_MOCKS, API_BASE_URL } from './lib/apiClient';
import type {
  AdherenceSummaryResponse,
  ApiErrorResponse,
  ConfirmDoseResponse,
  TodayAdherenceResponse,
} from './types/api';

const todaySample: TodayAdherenceResponse = adherenceMock.today.has_next_dose as TodayAdherenceResponse;
const confirmSample: ConfirmDoseResponse = adherenceMock.confirm.response as ConfirmDoseResponse;
const summarySample: AdherenceSummaryResponse = adherenceMock.summary as AdherenceSummaryResponse;
const overlapErrorSample: ApiErrorResponse = errorsMock['409_APPOINTMENT_OVERLAP'];

export default function App() {
  const [fontScale, setFontScale] = useState<number>(authMock.meByRole.PATIENT.uiFontScale);
  const [highContrast, setHighContrast] = useState<boolean>(authMock.meByRole.PATIENT.highContrast);

  useEffect(() => {
    document.documentElement.style.setProperty('--font-scale', String(fontScale));
    document.documentElement.setAttribute('data-high-contrast', String(highContrast));
  }, [fontScale, highContrast]);

  return (
    <div className="min-h-screen p-6 max-w-4xl mx-auto space-y-6">
      <header className="rounded-xl border border-[var(--border-subtle)] bg-[var(--bg-surface)] p-5 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Eye className="w-7 h-7 text-[var(--accent-primary)]" aria-hidden="true" />
          <div>
            <h1 className="text-xl font-bold">OphthaCloud Academic · Stage 0 Ready</h1>
            <p className="text-sm text-[var(--text-muted)]">
              Mod date: {USE_MOCKS ? 'Mock JSON activ' : 'API Live'} ({API_BASE_URL})
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3" role="region" aria-label="Bara de accesibilitate CIC-06.1">
          <button
            type="button"
            onClick={() => setFontScale((prev) => (prev >= 2 ? 1 : Number((prev + 0.25).toFixed(2))))}
            className="touch-target inline-flex items-center gap-2 rounded-lg border border-[var(--border-subtle)] px-4 py-2 font-medium"
          >
            <Type className="w-5 h-5" aria-hidden="true" />
            <span>Text: {Math.round(fontScale * 100)}%</span>
          </button>
          <button
            type="button"
            onClick={() => setHighContrast((prev) => !prev)}
            aria-pressed={highContrast}
            className="touch-target inline-flex items-center gap-2 rounded-lg bg-[var(--accent-primary)] text-[var(--accent-contrast)] px-4 py-2 font-medium"
          >
            <Contrast className="w-5 h-5" aria-hidden="true" />
            <span>Contrast ridicat: {highContrast ? 'ACTIV' : 'OPRIT'}</span>
          </button>
        </div>
      </header>

      <main className="rounded-xl border border-[var(--border-subtle)] bg-[var(--bg-surface)] p-6 space-y-4">
        <div className="flex items-center gap-2 font-semibold">
          <CheckCircle2 className="w-5 h-5 text-emerald-600" aria-hidden="true" />
          <span>Contracte canonice & Mock Kit verificate</span>
        </div>
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
          <div>
            <dt className="text-[var(--text-muted)]">Pacient canonic</dt>
            <dd className="font-mono">{receptionMock.createPatient.response.patientId}</dd>
          </div>
          <div>
            <dt className="text-[var(--text-muted)]">Plan activ</dt>
            <dd className="font-mono">{clinicalMock.createTreatmentPlan.response.planId}</dd>
          </div>
          <div>
            <dt className="text-[var(--text-muted)]">Următoarea doză ({todaySample.clinicDate})</dt>
            <dd>
              {todaySample.nextDose?.medicationName} · {todaySample.nextDose?.targetEye} ·{' '}
              {todaySample.nextDose?.localTime}
            </dd>
          </div>
          <div>
            <dt className="text-[var(--text-muted)]">Confirmare & Sumar</dt>
            <dd>
              {confirmSample.status} (+{confirmSample.totalPoints}p) · Aderență: {summarySample.adherencePercent}% ·
              Eroare exemplu: {overlapErrorSample.code}
            </dd>
          </div>
        </dl>
      </main>
    </div>
  );
}
