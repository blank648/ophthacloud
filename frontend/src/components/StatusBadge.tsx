import React from 'react';
import { statusLabels, statusStyles, clinicalFlagStyles, appointmentTypeLabels, appointmentTypeColors } from '@/data/demo-data';
import type { AppointmentStatus, AppointmentType } from '@/data/demo-data';

export const AppointmentStatusBadge: React.FC<{ status: AppointmentStatus }> = ({ status }) => {
  const style = statusStyles[status];
  return (
    <span
      className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold badge-text"
      style={{ backgroundColor: style.bg, color: style.text }}
    >
      {statusLabels[status]}
    </span>
  );
};

export const AppointmentTypeBadge: React.FC<{ type: AppointmentType }> = ({ type }) => {
  const color = appointmentTypeColors[type];
  return (
    <span
      className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-semibold badge-text"
      style={{ backgroundColor: `${color}1A`, color }}
    >
      {appointmentTypeLabels[type]}
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
