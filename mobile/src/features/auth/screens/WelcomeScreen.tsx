import React, {useState, useRef, useCallback} from 'react';
import {
  View,
  Text,
  StyleSheet,
  StatusBar,
  Modal,
  ActivityIndicator,
  Alert,
  Dimensions,
} from 'react-native';
import Animated, {FadeInDown, FadeIn, FadeInUp} from 'react-native-reanimated';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import WebView, {type WebViewNavigation} from 'react-native-webview';
import {useTheme} from '../../../theme';
import {Button} from '../../../components/Button';
import {GlowBlob, ScalePress} from '../../../components/AnimatedComponents';
import {useAuth} from '../hooks/useAuth';
import {GITHUB_CLIENT_ID, GITHUB_REDIRECT_URI} from '@env';

const {width: SCREEN_W} = Dimensions.get('window');
const GITHUB_SCOPE = 'read:user user:email';

const FEATURES = [
  {icon: '⚡', title: 'GitHub Powered', text: 'Auto-import repos, skills & stats'},
  {icon: '🎨', title: 'Pro Templates', text: 'Enhancv-quality, fully customizable'},
  {icon: '🤖', title: 'AI Enhanced', text: 'Smart bullet points & descriptions'},
  {icon: '🌐', title: 'One-Tap Share', text: 'Publish & share instantly'},
];

export default function WelcomeScreen() {
  const {theme} = useTheme();
  const insets = useSafeAreaInsets();
  const {loginWithGitHub, loading, error} = useAuth();

  const [showWebView, setShowWebView] = useState(false);
  const codeExtracted = useRef(false);

  const githubAuthUrl =
    `https://github.com/login/oauth/authorize` +
    `?client_id=${GITHUB_CLIENT_ID}` +
    `&redirect_uri=${encodeURIComponent(GITHUB_REDIRECT_URI)}` +
    `&scope=${encodeURIComponent(GITHUB_SCOPE)}`;

  const handleNavigationChange = useCallback(
    (event: WebViewNavigation) => {
      const url = event.url;
      if (url.startsWith(GITHUB_REDIRECT_URI) && !codeExtracted.current) {
        const codeMatch = url.match(/[?&]code=([^&]+)/);
        if (codeMatch) {
          codeExtracted.current = true;
          setShowWebView(false);
          loginWithGitHub(codeMatch[1], GITHUB_REDIRECT_URI);
        }
      }
    },
    [loginWithGitHub],
  );

  const handleShouldStartLoad = useCallback(
    (event: {url: string}) => {
      const url = event.url;
      if (url.startsWith(GITHUB_REDIRECT_URI) && !codeExtracted.current) {
        const codeMatch = url.match(/[?&]code=([^&]+)/);
        if (codeMatch) {
          codeExtracted.current = true;
          setShowWebView(false);
          loginWithGitHub(codeMatch[1], GITHUB_REDIRECT_URI);
        }
        return false;
      }
      return true;
    },
    [loginWithGitHub],
  );

  const handleOpenGitHub = () => {
    codeExtracted.current = false;
    setShowWebView(true);
  };

  React.useEffect(() => {
    if (error) {
      Alert.alert('Sign In Failed', error);
    }
  }, [error]);

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <StatusBar barStyle="light-content" backgroundColor={theme.colors.background} />

      {/* Animated ambient glows */}
      <GlowBlob
        color={theme.colors.primary}
        size={350}
        style={{top: -140}}
      />
      <GlowBlob
        color={theme.colors.accent ?? theme.colors.secondary}
        size={200}
        style={{top: 120, left: SCREEN_W * 0.6}}
      />

      {/* Top spacer */}
      <View style={{paddingTop: insets.top + 48}} />

      {/* Hero section — staggered entrance */}
      <Animated.View
        entering={FadeInDown.delay(100).duration(600).springify().damping(16)}
        style={styles.hero}>
        {/* Logo */}
        <Animated.View
          entering={FadeIn.delay(200).duration(500)}
          style={[
            styles.logoIconContainer,
            {
              backgroundColor: theme.colors.primary + '15',
              borderColor: theme.colors.primary + '30',
              shadowColor: theme.colors.primary,
              shadowOffset: {width: 0, height: 0},
              shadowOpacity: 0.3,
              shadowRadius: 24,
            },
          ]}>
          <Text style={[styles.logoIcon, {color: theme.colors.primary}]}>{'</>'}</Text>
        </Animated.View>

        <Animated.Text
          entering={FadeInDown.delay(300).duration(500).springify()}
          style={[styles.appName, {color: theme.colors.text}]}>
          OpenFolio
        </Animated.Text>

        <Animated.Text
          entering={FadeInDown.delay(400).duration(500).springify()}
          style={[styles.tagline, {color: theme.colors.textSecondary}]}>
          Your developer portfolio,{'\n'}crafted in minutes.
        </Animated.Text>
      </Animated.View>

      {/* Feature cards — staggered grid */}
      <View style={styles.featuresGrid}>
        {FEATURES.map((f, i) => (
          <Animated.View
            key={i}
            entering={FadeInDown.delay(500 + i * 80).duration(500).springify().damping(16)}
            style={[
              styles.featureCard,
              {
                backgroundColor: theme.colors.card ?? theme.colors.surface,
                borderColor: theme.colors.cardBorder ?? theme.colors.border,
              },
            ]}>
            <View style={[styles.featureIconWrap, {backgroundColor: theme.colors.primary + '15'}]}>
              <Text style={styles.featureIcon}>{f.icon}</Text>
            </View>
            <Text style={[styles.featureTitle, {color: theme.colors.text}]}>{f.title}</Text>
            <Text style={[styles.featureText, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
              {f.text}
            </Text>
          </Animated.View>
        ))}
      </View>

      {/* Bottom actions — slide up */}
      <Animated.View
        entering={FadeInUp.delay(900).duration(600).springify().damping(18)}
        style={[styles.actions, {paddingBottom: Math.max(insets.bottom + 24, 44)}]}>
        <Button
          label={loading ? 'Signing in…' : 'Continue with GitHub'}
          icon="🐙"
          onPress={handleOpenGitHub}
          loading={loading}
          disabled={loading}
          style={styles.githubButton}
          size="lg"
        />
        <Text style={[styles.disclaimer, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
          We'll import your public repos & profile to build{'\n'}your portfolio automatically. No data leaves your device.
        </Text>
      </Animated.View>

      {/* GitHub OAuth WebView Modal */}
      <Modal
        visible={showWebView}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setShowWebView(false)}>
        <View style={[styles.webViewContainer, {backgroundColor: theme.colors.background}]}>
          <View style={[styles.webViewHeader, {borderBottomColor: theme.colors.border}]}>
            <Text style={[styles.webViewTitle, {color: theme.colors.text}]}>
              Sign in with GitHub
            </Text>
            <Button
              label="Cancel"
              variant="ghost"
              onPress={() => setShowWebView(false)}
              size="sm"
              fullWidth={false}
              style={styles.cancelBtn}
            />
          </View>
          <WebView
            source={{uri: githubAuthUrl}}
            onNavigationStateChange={handleNavigationChange}
            onShouldStartLoadWithRequest={handleShouldStartLoad}
            startInLoadingState
            renderLoading={() => (
              <View style={styles.webViewLoading}>
                <ActivityIndicator size="large" color={theme.colors.primary} />
              </View>
            )}
            style={styles.webView}
          />
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  hero: {
    alignItems: 'center',
    paddingHorizontal: 24,
    gap: 12,
    marginBottom: 32,
  },
  logoIconContainer: {
    width: 80,
    height: 80,
    borderRadius: 24,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  logoIcon: {
    fontSize: 28,
    fontWeight: '700',
    letterSpacing: -1,
  },
  appName: {
    fontSize: 44,
    fontWeight: '800',
    letterSpacing: -2,
  },
  tagline: {
    fontSize: 17,
    lineHeight: 26,
    textAlign: 'center',
    fontWeight: '400',
  },
  featuresGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 20,
    gap: 10,
    justifyContent: 'center',
  },
  featureCard: {
    width: '47%',
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    gap: 8,
  },
  featureIconWrap: {
    width: 36,
    height: 36,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 2,
  },
  featureIcon: {
    fontSize: 18,
  },
  featureTitle: {
    fontSize: 14,
    fontWeight: '700',
    letterSpacing: -0.2,
  },
  featureText: {
    fontSize: 12,
    lineHeight: 17,
    fontWeight: '400',
  },
  actions: {
    paddingHorizontal: 24,
    gap: 14,
    alignItems: 'center',
    marginTop: 'auto',
  },
  githubButton: {
    width: '100%',
  },
  disclaimer: {
    fontSize: 12,
    textAlign: 'center',
    lineHeight: 18,
    fontWeight: '400',
  },
  webViewContainer: {
    flex: 1,
  },
  webViewHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
  },
  webViewTitle: {
    fontSize: 17,
    fontWeight: '600',
  },
  cancelBtn: {
    paddingHorizontal: 8,
  },
  webView: {
    flex: 1,
  },
  webViewLoading: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
