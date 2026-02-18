import type {NativeStackScreenProps} from '@react-navigation/native-stack';

export type AuthStackParamList = {
  Welcome: undefined;
};

export type AppStackParamList = {
  Dashboard: undefined;
  Editor: {portfolioId: number};
  Preview: {portfolioId: number};
  Publish: {portfolioId: number};
  Export: {portfolioId: number};
  ResumeBuilder: {portfolioId: number};
  ResumePreview: {resumeId: number};
  ResumeTemplates: {resumeId: number};
  ResumeEditor: {portfolioId: number};
};

export type RootStackParamList = {
  Auth: undefined;
  App: undefined;
};

export type AuthScreenProps<T extends keyof AuthStackParamList> =
  NativeStackScreenProps<AuthStackParamList, T>;

export type AppScreenProps<T extends keyof AppStackParamList> =
  NativeStackScreenProps<AppStackParamList, T>;
