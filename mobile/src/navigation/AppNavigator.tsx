import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import type {AppStackParamList} from './types';
import DashboardScreen from '../features/dashboard/screens/DashboardScreen';
import EditorScreen from '../features/editor/screens/EditorScreen';
import PreviewScreen from '../features/preview/screens/PreviewScreen';
import PublishScreen from '../features/publish/screens/PublishScreen';
import ExportScreen from '../features/export/screens/ExportScreen';
import ResumeBuilderScreen from '../features/resume/screens/ResumeBuilderScreen';
import ResumePreviewScreen from '../features/resume/screens/ResumePreviewScreen';
import ResumeTemplatesScreen from '../features/resume/screens/ResumeTemplatesScreen';
import ResumeEditorScreen from '../features/export/screens/ResumeEditorScreen';

const Stack = createNativeStackNavigator<AppStackParamList>();

export default function AppNavigator() {
  return (
    <Stack.Navigator screenOptions={{headerShown: false}}>
      <Stack.Screen name="Dashboard" component={DashboardScreen} />
      <Stack.Screen name="Editor" component={EditorScreen} />
      <Stack.Screen name="Preview" component={PreviewScreen} />
      <Stack.Screen name="Publish" component={PublishScreen} />
      <Stack.Screen name="Export" component={ExportScreen} />
      <Stack.Screen name="ResumeBuilder" component={ResumeBuilderScreen} />
      <Stack.Screen name="ResumePreview" component={ResumePreviewScreen} />
      <Stack.Screen name="ResumeTemplates" component={ResumeTemplatesScreen} />
      <Stack.Screen name="ResumeEditor" component={ResumeEditorScreen} />
    </Stack.Navigator>
  );
}
