import React, { useState } from 'react';
import { useApp, UserRole } from '@/contexts/AppContext';
import OphthaLogo from '@/components/OphthaLogo';
import { Eye, FileCheck, Shield, ChevronDown } from 'lucide-react';

const roles: { role: UserRole; label: string; color: string }[] = [
  { role: 'doctor', label: 'Doctor', color: '#13759C' },
  { role: 'receptionist', label: 'Recepție', color: '#10B981' },
  { role: 'manager', label: 'Manager', color: '#8B5CF6' },
  { role: 'optician', label: 'Optician', color: '#F59E0B' },
  { role: 'patient', label: 'Pacient', color: '#06B6D4' },
];

const LoginPage: React.FC = () => {
  const { login } = useApp();
  const [email, setEmail] = useState('dr.popescu@visiomed.ro');
  const [password, setPassword] = useState('••••••••');
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="min-h-screen flex">
      {/* Left brand panel */}
      <div className="hidden lg:flex lg:w-[45%] relative overflow-hidden flex-col items-center justify-center"
        style={{ background: 'linear-gradient(135deg, #0A3D52 0%, #106283 50%, #13759C 100%)' }}
      >
        {/* Decorative concentric circles */}
        <div className="absolute inset-0 flex items-center justify-center">
          {[280, 220, 160, 100].map((size) => (
            <div
              key={size}
              className="absolute rounded-full border border-white/5"
              style={{ width: size * 2, height: size * 2 }}
            />
          ))}
        </div>

        <div className="relative z-10 text-center px-12">
          <OphthaLogo size={80} showText={false} className="justify-center mb-6" />
          <h1 className="text-3xl font-bold text-white mb-2">OphthaCloud</h1>
          <p className="text-primary-300 text-lg mb-12 italic">Viziunea ta clinică. Digitalizată.</p>

          <div className="space-y-6 text-left max-w-xs mx-auto">
            {[
              { icon: Eye, text: 'Consultații complete HL7 FHIR' },
              { icon: FileCheck, text: 'ERP Optic integrat' },
              { icon: Shield, text: 'Conformitate GDPR' },
            ].map(({ icon: Ic, text }) => (
              <div key={text} className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-lg bg-white/10 flex items-center justify-center">
                  <Ic className="w-5 h-5 text-primary-300" />
                </div>
                <span className="text-white/90 text-[15px] font-medium">{text}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex flex-col items-center justify-center bg-card px-8">
        <div className="w-full max-w-[400px]">
          <div className="lg:hidden mb-8 flex justify-center">
            <OphthaLogo size={48} className="text-primary-900" />
          </div>
          <h2 className="text-clinical-xl font-bold text-foreground mb-1">Bun venit în OphthaCloud</h2>
          <p className="text-clinical-sm text-muted-foreground mb-8">Introduceți datele de autentificare</p>

          <div className="space-y-4">
            {/* Clinic selector */}
            <div>
              <label className="block text-clinical-sm font-semibold text-foreground mb-1.5">Clinica</label>
              <div className="relative">
                <select className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-base appearance-none cursor-pointer pr-8">
                  <option>Clinica Oftalmologică Visiomed</option>
                  <option>Centrul Optic Claritate</option>
                </select>
                <ChevronDown className="absolute right-3 top-3 w-4 h-4 text-muted-foreground pointer-events-none" />
              </div>
            </div>

            {/* Email */}
            <div>
              <label className="block text-clinical-sm font-semibold text-foreground mb-1.5">Email</label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-base focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors"
              />
            </div>

            {/* Password */}
            <div>
              <label className="block text-clinical-sm font-semibold text-foreground mb-1.5">Parolă</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  className="w-full h-10 rounded-md border border-input bg-background px-3 pr-16 text-clinical-base focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-2.5 text-clinical-xs text-primary font-medium"
                >
                  {showPassword ? 'Ascunde' : 'Arată'}
                </button>
              </div>
            </div>

            <button
              onClick={() => login('doctor')}
              className="w-full h-12 rounded-lg bg-primary text-white font-semibold text-clinical-base hover:bg-primary-700 transition-colors shadow-sm"
            >
              Autentificare
            </button>

            <button className="w-full text-center text-clinical-sm text-primary font-medium hover:underline">
              Ai uitat parola?
            </button>
          </div>

          {/* Demo role switcher */}
          <div className="mt-10 pt-6 border-t border-border">
            <p className="text-clinical-xs text-muted-foreground mb-3 text-center font-medium">Demo rapid:</p>
            <div className="flex flex-wrap justify-center gap-2">
              {roles.map(({ role, label, color }) => (
                <button
                  key={role}
                  onClick={() => login(role)}
                  className="px-4 py-2 rounded-full text-[12px] font-semibold text-white transition-all hover:opacity-90 hover:scale-105"
                  style={{ backgroundColor: color }}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <p className="mt-8 text-center text-[10px] text-muted-foreground">
            v2.1.0 · Datele sunt procesate conform GDPR (Regulamentul UE 2016/679)
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
