import React from 'react';
import { LucideIcon } from 'lucide-react';

interface KPICardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  trend?: { value: string; positive: boolean };
  icon: LucideIcon;
  iconColor?: string;
}

const KPICard: React.FC<KPICardProps> = ({ title, value, subtitle, trend, icon: Icon, iconColor }) => (
  <div className="bg-card rounded-xl border border-border shadow-sm p-5 hover:shadow-md hover:border-l-[3px] hover:border-l-primary transition-all duration-200 group">
    <div className="flex items-start justify-between">
      <div className="flex-1">
        <p className="text-clinical-sm text-muted-foreground font-medium">{title}</p>
        <p className="text-clinical-2xl font-bold text-foreground mt-1">{value}</p>
        {subtitle && <p className="text-clinical-xs text-muted-foreground mt-0.5">{subtitle}</p>}
        {trend && (
          <p className={`text-clinical-xs font-semibold mt-1 ${trend.positive ? 'text-clinical-success' : 'text-clinical-danger'}`}>
            {trend.positive ? '▲' : '▼'} {trend.value}
          </p>
        )}
      </div>
      <div
        className="w-10 h-10 rounded-lg flex items-center justify-center"
        style={{ backgroundColor: iconColor ? `${iconColor}1A` : 'hsl(var(--color-primary-100))' }}
      >
        <Icon className="w-5 h-5" style={{ color: iconColor || 'hsl(var(--color-primary-600))' }} />
      </div>
    </div>
  </div>
);

export default KPICard;
