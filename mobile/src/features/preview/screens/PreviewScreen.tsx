import React, {useCallback, useEffect, useRef, useState} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  StatusBar,
  Alert,
} from 'react-native';
import Animated, {FadeIn, FadeInDown, FadeInUp} from 'react-native-reanimated';
import {WebView} from 'react-native-webview';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import type {AppScreenProps} from '../../../navigation/types';
import {useTheme} from '../../../theme';
import {usePortfolioStore} from '../../dashboard/store/portfolioStore';
import {previewService} from '../services/previewService';
import {publishService} from '../../publish/services/publishService';
import {PulseView} from '../../../components/AnimatedComponents';

type Props = AppScreenProps<'Preview'>;
type ThemeKey = 'dark' | 'minimal' | 'hacker';

const THEMES: {key: ThemeKey; label: string; emoji: string; color: string}[] = [
  {key: 'dark', label: 'Dark', emoji: '🌙', color: '#A78BFA'},
  {key: 'minimal', label: 'Light', emoji: '☀️', color: '#F59E0B'},
  {key: 'hacker', label: 'Matrix', emoji: '💻', color: '#00FF41'},
];

export default function PreviewScreen({route, navigation}: Props) {
  const {portfolioId} = route.params;
  const {theme} = useTheme();
  const insets = useSafeAreaInsets();
  const webViewRef = useRef<WebView>(null);

  const portfolio = usePortfolioStore(s => s.portfolios.find(p => p.id === portfolioId));
  const fetchPortfolios = usePortfolioStore(s => s.fetchPortfolios);

  const [html, setHtml] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTheme, setActiveTheme] = useState<ThemeKey>(
    (portfolio?.themeKey as ThemeKey) ?? 'dark',
  );
  const [switching, setSwitching] = useState(false);

  const loadPreview = useCallback(async () => {
    setLoading(true);
    try {
      const content = await previewService.getHtml(portfolioId);
      setHtml(content);
    } catch (e: any) {
      Alert.alert('Error', e?.message ?? 'Failed to load preview');
    } finally {
      setLoading(false);
    }
  }, [portfolioId]);

  useEffect(() => {
    loadPreview();
  }, [loadPreview]);

  const switchTheme = async (key: ThemeKey) => {
    if (switching || key === activeTheme) return;
    setSwitching(true);
    try {
      await publishService.updateTheme(portfolioId, key);
      setActiveTheme(key);
      await fetchPortfolios();
      await loadPreview();
    } catch (e: any) {
      Alert.alert('Error', e?.message ?? 'Failed to switch theme');
    } finally {
      setSwitching(false);
    }
  };

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <StatusBar barStyle="light-content" />

      {/* Header */}
      <Animated.View
        entering={FadeIn.duration(300)}
        style={[styles.header, {paddingTop: insets.top + 12, borderBottomColor: theme.colors.border}]}>
        <TouchableOpacity onPress={() => navigation.goBack()} activeOpacity={0.7}>
          <Text style={[styles.backText, {color: theme.colors.primary}]}>←</Text>
        </TouchableOpacity>
        <Text style={[styles.headerTitle, {color: theme.colors.text}]} numberOfLines={1}>
          Preview
        </Text>
        <TouchableOpacity
          onPress={() => navigation.navigate('Publish', {portfolioId})}
          activeOpacity={0.8}
          style={[styles.publishBtn, {
            backgroundColor: theme.colors.primary,
            shadowColor: theme.colors.primary,
            shadowOffset: {width: 0, height: 3},
            shadowOpacity: 0.3,
            shadowRadius: 6,
          }]}>
          <Text style={styles.publishBtnText}>Publish ↗</Text>
        </TouchableOpacity>
      </Animated.View>

      {/* Theme switcher */}
      <Animated.View
        entering={FadeInDown.delay(100).duration(400).springify()}
        style={[styles.themeSwitcher, {borderBottomColor: theme.colors.borderLight ?? theme.colors.border}]}>
        {THEMES.map((t, i) => {
          const isActive = activeTheme === t.key;
          return (
            <Animated.View
              key={t.key}
              entering={FadeInDown.delay(i * 80 + 150).duration(400).springify()}>
              <TouchableOpacity
                onPress={() => switchTheme(t.key)}
                activeOpacity={0.7}
                style={[styles.themeChip, {
                  backgroundColor: isActive ? t.color + '18' : 'transparent',
                  borderColor: isActive ? t.color : theme.colors.borderLight ?? theme.colors.border,
                  shadowColor: isActive ? t.color : 'transparent',
                  shadowOffset: {width: 0, height: isActive ? 2 : 0},
                  shadowOpacity: isActive ? 0.2 : 0,
                  shadowRadius: isActive ? 6 : 0,
                }]}>
                <Text style={styles.themeEmoji}>{t.emoji}</Text>
                <Text style={[styles.themeLabel, {color: isActive ? t.color : theme.colors.textSecondary}]}>
                  {t.label}
                </Text>
                {isActive && (
                  <View style={[styles.themeActiveDot, {backgroundColor: t.color}]} />
                )}
              </TouchableOpacity>
            </Animated.View>
          );
        })}
      </Animated.View>

      {/* WebView */}
      <View style={styles.webviewContainer}>
        {(loading || switching) && (
          <Animated.View
            entering={FadeIn.duration(300)}
            style={[styles.loadingOverlay, {backgroundColor: theme.colors.background}]}>
            <PulseView>
              <View style={[styles.loadingIcon, {backgroundColor: theme.colors.primary + '12', borderColor: theme.colors.primary + '25'}]}>
                <Text style={{fontSize: 32}}>{switching ? '🎨' : '🌐'}</Text>
              </View>
            </PulseView>
            <Text style={[styles.loadingText, {color: theme.colors.textSecondary}]}>
              {switching ? 'Applying theme...' : 'Rendering portfolio...'}
            </Text>
          </Animated.View>
        )}
        {html && !switching ? (
          <Animated.View entering={FadeIn.delay(100).duration(400)} style={{flex: 1}}>
            <WebView
              ref={webViewRef}
              source={{html, baseUrl: 'about:blank'}}
              style={{flex: 1, backgroundColor: theme.colors.background}}
              scrollEnabled
              showsVerticalScrollIndicator={false}
              originWhitelist={['*']}
              javaScriptEnabled={false}
              onShouldStartLoadWithRequest={req => req.url === 'about:blank'}
            />
          </Animated.View>
        ) : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1},
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
  publishBtn: {paddingHorizontal: 18, paddingVertical: 8, borderRadius: 22},
  publishBtnText: {color: '#fff', fontSize: 14, fontWeight: '800'},
  themeSwitcher: {
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
  },
  themeChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 100,
    borderWidth: 1.5,
  },
  themeEmoji: {fontSize: 15},
  themeLabel: {fontSize: 13, fontWeight: '700'},
  themeActiveDot: {width: 6, height: 6, borderRadius: 3},
  webviewContainer: {flex: 1},
  loadingOverlay: {
    ...StyleSheet.absoluteFillObject,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
    zIndex: 10,
  },
  loadingIcon: {
    width: 72,
    height: 72,
    borderRadius: 20,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  loadingText: {fontSize: 14, fontWeight: '600'},
});

