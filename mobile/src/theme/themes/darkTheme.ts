import {tokens} from '../tokens';

export const darkTheme = {
  ...tokens,
  colors: {
    ...tokens.colors,
    primary: '#8B5CF6',
    primaryDark: '#7C3AED',
    primaryLight: '#A78BFA',
    secondary: '#06B6D4',
    accent: '#EC4899',
    background: '#09090B',
    surface: '#111113',
    surfaceElevated: '#18181B',
    card: '#16161E',
    cardBorder: 'rgba(255,255,255,0.06)',
    cardHover: '#1E1E2A',
    text: '#F4F4F5',
    textSecondary: '#A1A1AA',
    textMuted: '#52525B',
    border: '#27272A',
    borderLight: '#1E1E22',
    error: '#F87171',
    success: '#4ADE80',
    warning: '#FBBF24',
    info: '#60A5FA',
    overlay: 'rgba(0,0,0,0.7)',
    // Gradient endpoints
    gradientStart: '#8B5CF6',
    gradientEnd: '#06B6D4',
    gradientAccent: '#EC4899',
    // Shadows
    shadowColor: '#8B5CF6',
  },
} as const;
