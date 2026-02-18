import {tokens} from '../tokens';

export const lightTheme = {
  ...tokens,
  colors: {
    ...tokens.colors,
    primary: '#6C63FF',
    primaryDark: '#5A52D5',
    primaryLight: '#8B85FF',
    secondary: '#F5A623',
    accent: '#EC4899',
    background: '#FFFFFF',
    surface: '#F8F9FA',
    surfaceElevated: '#FFFFFF',
    card: '#FFFFFF',
    cardBorder: '#E5E7EB',
    cardHover: '#F3F4F6',
    text: '#1A1A2E',
    textSecondary: '#6C757D',
    textMuted: '#9CA3AF',
    border: '#E9ECEF',
    borderLight: '#F3F4F6',
    error: '#DC3545',
    success: '#28A745',
    warning: '#F59E0B',
    info: '#3B82F6',
    overlay: 'rgba(0,0,0,0.4)',
    gradientStart: '#6C63FF',
    gradientEnd: '#F5A623',
    gradientAccent: '#EC4899',
    shadowColor: '#6C63FF',
  },
} as const;

export type AppTheme = typeof lightTheme;
