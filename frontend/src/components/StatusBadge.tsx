import React from 'react';
import { statusLabels, statusStyles, clinicalFlagStyles, appointmentTypeLabels, appointmentTypeColors } from '@/data/demo-data';
import type { AppointmentStatus, AppointmentType } from '@/data/demo-data';

export const AppointmentStatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const normalized = status ? status.toLowerCase() : '';
  const style = statusStyles[normalized] || { bg: '#E5E7EB', text: '#374151' };
  const label = statusLabels[normalized] || status;
  return (
    <span
      className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold badge-text"
      style={{ backgroundColor: style.bg, color: style.text }}
    >
      {label}
    </span>
  );
};

export const AppointmentTypeBadge: React.FC<{ name: string; color: string }> = ({ name, color }) => {
  return (
    <span
      className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold badge-text"
      style={{ backgroundColor: `${color}1A`, color }}
    >
      {name}
    </span>
  );
};

export const ClinicalFlagBadge: React.FC<{ flag: string }> = ({ flag }) => {
  const style = clinicalFlagStyles[flag] || clinicalFlagStyles.NORMAL;
  return (
    <span
      className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold badge-text clinical-label"
      style={{ backgroundColor: style.bg, color: style.text, border: `1px solid ${style.border}` }}
    >
      {flag}
    </span>
  );
};

export const IOPValue: React.FC<{ value: number }> = ({ value }) => {
  let className = 'font-clinical font-semibold';
  if (value <= 21) className += ' iop-normal';
  else if (value <= 25) className += ' iop-warning';
  else className += ' iop-danger';

  return (
    <span className={className}>
      {value} mmHg
      {value > 21 && ' ⚠'}
    </span>
  );
};
