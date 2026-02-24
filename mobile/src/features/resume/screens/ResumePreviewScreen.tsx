import React, {useEffect, useState, useRef, useCallback} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  StatusBar,
  Alert,
  Share,
} from 'react-native';
import Animated, {FadeIn, FadeInUp, FadeInDown} from 'react-native-reanimated';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {WebView} from 'react-native-webview';
import type {AppScreenProps} from '../../../navigation/types';
import {useTheme} from '../../../theme';
import {resumeService} from '../services/resumeService';
import {PulseView} from '../../../components/AnimatedComponents';

type Props = AppScreenProps<'ResumePreview'>;

export default function ResumePreviewScreen({route, navigation}: Props) {
  const {resumeId} = route.params;
  const {theme} = useTheme();
  const insets = useSafeAreaInsets();
  const webViewRef = useRef<WebView>(null);

  const [html, setHtml] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sharing, setSharing] = useState(false);

  const loadPreview = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const previewHtml = await resumeService.getPreviewHtml(resumeId);
      setHtml(wrapInPdfPageView(previewHtml));
    } catch (e: any) {
      setError(e?.message ?? 'Failed to load preview');
    } finally {
      setLoading(false);
    }
  }, [resumeId]);

  useEffect(() => {
    loadPreview();
  }, [loadPreview]);

  const handleShare = async () => {
    setSharing(true);
    try {
      const result = await resumeService.generatePdf(resumeId);
      await Share.share({
        url: result.downloadUrl,
        message: `My Resume: ${result.downloadUrl}`,
        title: 'My Resume',
      });
    } catch (e: any) {
      Alert.alert('Error', e?.message ?? 'Failed to share resume');
    } finally {
      setSharing(false);
    }
  };

  const handleDownloadPdf = async () => {
    setSharing(true);
    try {
      const result = await resumeService.generatePdf(resumeId);
      Alert.alert('PDF Ready! 📄', 'Your resume PDF has been generated.', [
        {
          text: 'Share',
          onPress: () =>
            Share.share({url: result.downloadUrl, message: result.downloadUrl}),
        },
        {text: 'OK', style: 'cancel'},
      ]);
    } catch (e: any) {
      Alert.alert('Error', e?.message ?? 'Failed to generate PDF');
    } finally {
      setSharing(false);
    }
  };

  return (
    <View style={[styles.container, {backgroundColor: '#1A1A1E'}]}>
      <StatusBar barStyle="light-content" />

      {/* Header */}
      <Animated.View
        entering={FadeIn.duration(300)}
        style={[styles.header, {
          paddingTop: insets.top + 12,
          backgroundColor: '#0F0F12F8',
          borderBottomColor: '#2A2A30',
        }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} activeOpacity={0.7}>
          <Text style={[styles.backText, {color: theme.colors.primary}]}>←</Text>
        </TouchableOpacity>
        <Text style={[styles.headerTitle, {color: '#FAFAFA'}]}>Resume Preview</Text>
        <TouchableOpacity onPress={handleShare} activeOpacity={0.7} disabled={sharing}>
          {sharing ? (
            <ActivityIndicator size="small" color={theme.colors.primary} />
          ) : (
            <Text style={{fontSize: 18}}>📤</Text>
          )}
        </TouchableOpacity>
      </Animated.View>

      {/* Content */}
      {loading ? (
        <Animated.View entering={FadeIn.delay(100).duration(400)} style={styles.centered}>
          <PulseView>
            <View style={[styles.loadingIcon, {backgroundColor: theme.colors.primary + '12', borderColor: theme.colors.primary + '25'}]}>
              <Text style={{fontSize: 36}}>📄</Text>
            </View>
          </PulseView>
          <Text style={{color: '#9CA3AF', fontSize: 15, fontWeight: '600', marginTop: 16}}>
            Generating preview...
          </Text>
          <Text style={{color: '#6B7280', fontSize: 13, marginTop: 4}}>
            Rendering your resume template
          </Text>
        </Animated.View>
      ) : error ? (
        <Animated.View entering={FadeInDown.duration(400).springify()} style={styles.centered}>
          <View style={[styles.errorIcon, {backgroundColor: '#EF444415', borderColor: '#EF444425'}]}>
            <Text style={{fontSize: 36}}>⚠️</Text>
          </View>
          <Text style={{color: '#F87171', fontSize: 16, fontWeight: '700', marginTop: 16}}>
            Preview Failed
          </Text>
          <Text style={{color: '#9CA3AF', fontSize: 13, textAlign: 'center', paddingHorizontal: 40, marginTop: 4}}>
            {error}
          </Text>
          <TouchableOpacity
            onPress={loadPreview}
            activeOpacity={0.7}
            style={[styles.retryBtn, {backgroundColor: theme.colors.primary + '15', borderColor: theme.colors.primary + '30'}]}>
            <Text style={{color: theme.colors.primary, fontWeight: '700'}}>🔄 Retry</Text>
          </TouchableOpacity>
        </Animated.View>
      ) : (
        <Animated.View entering={FadeIn.delay(100).duration(400)} style={styles.pdfContainer}>
          <WebView
            ref={webViewRef}
            originWhitelist={['*']}
            source={{html: html ?? ''}}
            style={styles.webView}
            scrollEnabled={true}
            bounces={true}
            scalesPageToFit={false}
            javaScriptEnabled={true}
            showsVerticalScrollIndicator={true}
            contentMode="recommended"
            startInLoadingState={true}
            renderLoading={() => (
              <View style={[styles.centered, StyleSheet.absoluteFill, {backgroundColor: '#1A1A1E'}]}>
                <ActivityIndicator size="large" color={theme.colors.primary} />
              </View>
            )}
          />
        </Animated.View>
      )}

      {/* Footer */}
      <Animated.View
        entering={FadeInUp.delay(300).duration(400).springify()}
        style={[styles.footer, {
          paddingBottom: insets.bottom + 12,
          backgroundColor: '#0F0F12F5',
          borderTopColor: '#2A2A30',
        }]}>
        <TouchableOpacity
          onPress={() => navigation.navigate('ResumeTemplates', {resumeId})}
          activeOpacity={0.8}
          style={[styles.footerBtn, {backgroundColor: '#1E1E24', borderColor: '#2E2E35'}]}>
          <Text style={styles.footerBtnIcon}>🎨</Text>
          <Text style={[styles.footerBtnText, {color: '#E5E7EB'}]}>Template</Text>
        </TouchableOpacity>
        <TouchableOpacity
          onPress={loadPreview}
          disabled={loading}
          activeOpacity={0.8}
          style={[styles.footerBtn, {backgroundColor: '#1E1E24', borderColor: '#2E2E35', opacity: loading ? 0.5 : 1}]}>
          {loading ? (
            <ActivityIndicator size="small" color={theme.colors.primary} />
          ) : (
            <>
              <Text style={styles.footerBtnIcon}>🔄</Text>
              <Text style={[styles.footerBtnText, {color: '#E5E7EB'}]}>Refresh</Text>
            </>
          )}
        </TouchableOpacity>
        <TouchableOpacity
          onPress={handleDownloadPdf}
          disabled={sharing}
          activeOpacity={0.8}
          style={[styles.footerBtn, {
            backgroundColor: theme.colors.primary,
            borderColor: theme.colors.primary,
            shadowColor: theme.colors.primary,
            shadowOffset: {width: 0, height: 4},
            shadowOpacity: 0.4,
            shadowRadius: 10,
            opacity: sharing ? 0.6 : 1,
          }]}>
          {sharing ? (
            <ActivityIndicator size="small" color="#fff" />
          ) : (
            <>
              <Text style={styles.footerBtnIcon}>📥</Text>
              <Text style={[styles.footerBtnText, {color: '#fff'}]}>Get PDF</Text>
            </>
          )}
        </TouchableOpacity>
      </Animated.View>
    </View>
  );
}

function wrapInPdfPageView(innerHtml: string): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=3,user-scalable=yes">
  <style>
    html, body {
      margin: 0;
      padding: 0;
      background: #1A1A1E;
      min-height: 100vh;
    }
    .pdf-page-wrapper {
      display: flex;
      justify-content: center;
      padding: 20px 12px;
      min-height: 100vh;
    }
    .pdf-page {
      background: #FFFFFF;
      width: 100%;
      max-width: 680px;
      box-shadow: 0 8px 40px rgba(0,0,0,0.5), 0 2px 8px rgba(0,0,0,0.3);
      border-radius: 4px;
      overflow: hidden;
    }
  </style>
</head>
<body>
  <div class="pdf-page-wrapper">
    <div class="pdf-page">
      <iframe srcdoc='${innerHtml
        .replace(/'/g, '&#39;')
        .replace(/\n/g, ' ')}' 
        style="width:100%;border:none;min-height:100vh;"
        scrolling="no"
        onload="this.style.height=this.contentDocument.body.scrollHeight+'px'">
      </iframe>
    </div>
  </div>
</body>
</html>`;
}

const styles = StyleSheet.create({
  container: {flex: 1},
  centered: {flex: 1, alignItems: 'center', justifyContent: 'center'},
  header: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: 20,
    paddingBottom: 14,
    borderBottomWidth: 1,
    gap: 12,
  },
  backText: {fontSize: 26, fontWeight: '300', lineHeight: 30},
  headerTitle: {flex: 1, fontSize: 20, fontWeight: '800', letterSpacing: -0.5},
  loadingIcon: {
    width: 80,
    height: 80,
    borderRadius: 24,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  errorIcon: {
    width: 80,
    height: 80,
    borderRadius: 24,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  retryBtn: {
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 12,
    borderWidth: 1,
    marginTop: 16,
  },
  pdfContainer: {flex: 1},
  webView: {flex: 1, backgroundColor: '#1A1A1E'},
  footer: {
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 16,
    paddingTop: 12,
    borderTopWidth: 1,
  },
  footerBtn: {
    flex: 1,
    height: 48,
    borderRadius: 14,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: 4,
  },
  footerBtnIcon: {fontSize: 14},
  footerBtnText: {fontSize: 12, fontWeight: '700'},
});
