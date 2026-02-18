import React, {createContext, useContext, useState} from 'react';
import {lightTheme} from './themes/lightTheme';
import {darkTheme} from './themes/darkTheme';
import {hackerTheme} from './themes/hackerTheme';

export type ThemeKey = 'light' | 'dark' | 'hacker';

export type AppTheme = {
  colors: {
    primary: string;
    primaryDark: string;
    primaryLight?: string;
    secondary: string;
    accent?: string;
    background: string;
    surface: string;
    surfaceElevated?: string;
    card?: string;
    cardBorder?: string;
    cardHover?: string;
    text: string;
    textSecondary: string;
    textMuted?: string;
    border: string;
    borderLight?: string;
    error: string;
    success: string;
    warning?: string;
    info?: string;
    overlay: string;
    gradientStart?: string;
    gradientEnd?: string;
    gradientAccent?: string;
    shadowColor?: string;
  };
  spacing: {xs: number; sm: number; md: number; lg: number; xl: number; xxl: number};
  radii: {sm: number; md: number; lg: number; xl: number; full: number};
  fontSize: {xs: number; sm: number; md: number; lg: number; xl: number; xxl: number};
  fontWeight: {
    regular: '400';
    medium: '500';
    semibold: '600';
    bold: '700';
  };
};

const themeMap: Record<ThemeKey, AppTheme> = {
  light: lightTheme as AppTheme,
  dark: darkTheme as AppTheme,
  hacker: hackerTheme as AppTheme,
};

type ThemeContextValue = {
  theme: AppTheme;
  themeKey: ThemeKey;
  setTheme: (key: ThemeKey) => void;
};

const ThemeContext = createContext<ThemeContextValue>({
  theme: themeMap.dark,
  themeKey: 'dark',
  setTheme: () => {},
});

export function ThemeProvider({children}: {children: React.ReactNode}) {
  const [themeKey, setThemeKey] = useState<ThemeKey>('dark');

  return (
    <ThemeContext.Provider
      value={{
        theme: themeMap[themeKey],
        themeKey,
        setTheme: setThemeKey,
      }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  return useContext(ThemeContext);
}
