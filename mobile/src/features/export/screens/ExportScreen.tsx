import React, {useState, useEffect, useCallback, useRef} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  Alert,
  StatusBar,
  Share,
  TextInput,
  Image,
  Dimensions,
} from 'react-native';
import Animated, {
  FadeInDown,
  FadeIn,
  FadeInUp,
  FadeOut,
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSpring,
  withRepeat,
  withSequence,
  Easing,
  interpolate,
  runOnJS,
} from 'react-native-reanimated';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import WebView from 'react-native-webview';
import {launchImageLibrary} from 'react-native-image-picker';
import type {AppScreenProps} from '../../../navigation/types';
import {useTheme} from '../../../theme';
import {usePortfolioStore} from '../../dashboard/store/portfolioStore';
import {useAuthStore} from '../../auth/store/authStore';
import {exportService} from '../services/exportService';
import type {ResumeOptions, SavedResumeInfo} from '../services/exportService';
import {Skeleton, PulseView} from '../../../components/AnimatedComponents';
import RNFS from 'react-native-fs';
import Clipboard from '@react-native-clipboard/clipboard';

type Props = AppScreenProps<'Export'>;
type Template = 'pdf' | 'minimal' | 'dark' | 'hacker';
type ViewMode = 'settings' | 'preview' | 'pdfViewer';

const TEMPLATES: {
  key: Template;
  name: string;
  desc: string;
  accent: string;
  emoji: string;
}[] = [
  {key: 'pdf', name: 'Clean', desc: 'Professional blue accent', accent: '#2D5BFF', emoji: '📋'},
  {key: 'minimal', name: 'Minimal', desc: 'Elegant serif typography', accent: '#6B21A8', emoji: '✨'},
  {key: 'dark', name: 'Dark', desc: 'Modern developer style', accent: '#A78BFA', emoji: '🌙'},
  {key: 'hacker', name: 'Matrix', desc: 'Terminal hacker look', accent: '#00FF41', emoji: '💻'},
];

// ─── AI Loading Stages ──────────────────────────────────────
const LOADING_STAGES = [
  {emoji: '🔍', text: 'Analyzing your GitHub profile...', sub: 'Reading repos, commits & contributions'},
  {emoji: '🧠', text: 'AI is crafting your story...', sub: 'Turning code into career highlights'},
  {emoji: '✍️', text: 'Writing bullet points...', sub: 'Action verbs, metrics & impact statements'},
  {emoji: '📝', text: 'Polishing your summary...', sub: 'Professional third-person narrative'},
  {emoji: '🎨', text: 'Applying template design...', sub: 'Layout, typography & color scheme'},
  {emoji: '🔥', text: 'Almost there...', sub: 'Final touches & quality check'},
  {emoji: '✨', text: 'Just a moment more...', sub: 'Your resume is looking incredible'},
  {emoji: '🚀', text: 'Wrapping it up...', sub: 'Preparing the final preview'},
];

function AILoadingOverlay({primaryColor}: {primaryColor: string}) {
  const [stageIndex, setStageIndex] = useState(0);
  const progress = useSharedValue(0);
  const bounce = useSharedValue(0);
  const glow = useSharedValue(0.3);
  const dotScale1 = useSharedValue(0.4);
  const dotScale2 = useSharedValue(0.4);
  const dotScale3 = useSharedValue(0.4);

  useEffect(() => {
    // Progress bar — creep forward, slowing as it goes
    progress.value = withTiming(0.92, {duration: 60000, easing: Easing.out(Easing.cubic)});

    // Emoji bounce
    bounce.value = withRepeat(
      withSequence(
        withTiming(-12, {duration: 500, easing: Easing.out(Easing.cubic)}),
        withTiming(0, {duration: 500, easing: Easing.in(Easing.cubic)}),
      ), -1, false
    );

    // Glow pulse
    glow.value = withRepeat(
      withSequence(
        withTiming(0.6, {duration: 1200, easing: Easing.inOut(Easing.ease)}),
        withTiming(0.2, {duration: 1200, easing: Easing.inOut(Easing.ease)}),
      ), -1, false
    );

    // Dot animation (wave)
    const animateDots = () => {
      dotScale1.value = withRepeat(withSequence(
        withTiming(1, {duration: 400}), withTiming(0.4, {duration: 400})
      ), -1, false);
      setTimeout(() => {
        dotScale2.value = withRepeat(withSequence(
          withTiming(1, {duration: 400}), withTiming(0.4, {duration: 400})
        ), -1, false);
      }, 150);
      setTimeout(() => {
        dotScale3.value = withRepeat(withSequence(
          withTiming(1, {duration: 400}), withTiming(0.4, {duration: 400})
        ), -1, false);
      }, 300);
    };
    animateDots();

    // Cycle through stages
    const interval = setInterval(() => {
      setStageIndex(prev => (prev + 1) % LOADING_STAGES.length);
    }, 4000);

    return () => clearInterval(interval);
  }, []);

  const stage = LOADING_STAGES[stageIndex];

  const progressStyle = useAnimatedStyle(() => ({
    width: `${progress.value * 100}%`,
  }));

  const bounceStyle = useAnimatedStyle(() => ({
    transform: [{translateY: bounce.value}],
  }));

  const glowStyle = useAnimatedStyle(() => ({
    opacity: glow.value,
    transform: [{scale: interpolate(glow.value, [0.2, 0.6], [1, 1.15])}],
  }));

  const dot1Style = useAnimatedStyle(() => ({transform: [{scale: dotScale1.value}], opacity: dotScale1.value}));
  const dot2Style = useAnimatedStyle(() => ({transform: [{scale: dotScale2.value}], opacity: dotScale2.value}));
  const dot3Style = useAnimatedStyle(() => ({transform: [{scale: dotScale3.value}], opacity: dotScale3.value}));

  return (
    <View style={loadStyles.container}>
      {/* Ambient glow */}
      <Animated.View style={[loadStyles.glow, {backgroundColor: primaryColor}, glowStyle]} />

      {/* Bouncing emoji */}
      <Animated.View style={bounceStyle}>
        <View style={[loadStyles.emojiCircle, {borderColor: primaryColor + '40'}]}>
          <Animated.Text
            key={stage.emoji}
            entering={FadeIn.duration(300)}
            exiting={FadeOut.duration(200)}
            style={loadStyles.emoji}>
            {stage.emoji}
          </Animated.Text>
        </View>
      </Animated.View>

      {/* Stage text */}
      <Animated.Text
        key={stage.text}
        entering={FadeIn.duration(400)}
        exiting={FadeOut.duration(200)}
        style={loadStyles.stageText}>
        {stage.text}
      </Animated.Text>

      <Animated.Text
        key={stage.sub}
        entering={FadeIn.delay(100).duration(400)}
        exiting={FadeOut.duration(200)}
        style={loadStyles.stageSub}>
        {stage.sub}
      </Animated.Text>

      {/* Progress bar */}
      <View style={loadStyles.progressTrack}>
        <Animated.View style={[loadStyles.progressFill, {backgroundColor: primaryColor}, progressStyle]} />
      </View>

      {/* Animated dots */}
      <View style={loadStyles.dotsRow}>
        <Animated.View style={[loadStyles.dot, {backgroundColor: primaryColor}, dot1Style]} />
        <Animated.View style={[loadStyles.dot, {backgroundColor: primaryColor}, dot2Style]} />
        <Animated.View style={[loadStyles.dot, {backgroundColor: primaryColor}, dot3Style]} />
      </View>

      <Text style={loadStyles.tipText}>
        ⚡ Results are cached — next time will be instant
      </Text>
    </View>
  );
}

const loadStyles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 40,
  },
  glow: {
    position: 'absolute',
    width: 180,
    height: 180,
    borderRadius: 90,
    zIndex: -1,
  },
  emojiCircle: {
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: 'rgba(255,255,255,0.04)',
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 24,
  },
  emoji: {fontSize: 38},
  stageText: {
    color: '#F4F4F5',
    fontSize: 18,
    fontWeight: '700',
    textAlign: 'center',
    letterSpacing: -0.3,
    marginBottom: 6,
  },
  stageSub: {
    color: '#71717A',
    fontSize: 13,
    textAlign: 'center',
    marginBottom: 28,
    lineHeight: 18,
  },
  progressTrack: {
    width: '100%',
    height: 4,
    backgroundColor: 'rgba(255,255,255,0.06)',
    borderRadius: 2,
    overflow: 'hidden',
    marginBottom: 20,
  },
  progressFill: {
    height: '100%',
    borderRadius: 2,
  },
  dotsRow: {
    flexDirection: 'row',
    gap: 6,
    marginBottom: 24,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  tipText: {
    color: '#52525B',
    fontSize: 11,
    textAlign: 'center',
    fontWeight: '500',
  },
});

export default function ExportScreen({route, navigation}: Props) {
  const {portfolioId} = route.params;
  const {theme} = useTheme();
  const insets = useSafeAreaInsets();

  const portfolio = usePortfolioStore(s =>
    s.portfolios.find(p => p.id === portfolioId),
  );
  const githubUsername = useAuthStore(s => s.githubUsername);
  const githubAvatarUrl = githubUsername ? `https://github.com/${githubUsername}.png` : undefined;

  const [selected, setSelected] = useState<Template>('pdf');
  const [viewMode, setViewMode] = useState<ViewMode>('settings');

  // Resume options
  const [includePhoto, setIncludePhoto] = useState(true);
  const [photoSource, setPhotoSource] = useState<'github' | 'upload'>('github');
  const [uploadedPhotoUri, setUploadedPhotoUri] = useState<string | null>(null);
  const [phone, setPhone] = useState('');
  const [linkedIn, setLinkedIn] = useState('');
  const [website, setWebsite] = useState('');

  // Preview state
  const [previewHtml, setPreviewHtml] = useState<string | null>(null);
  const [loadingPreview, setLoadingPreview] = useState(false);

  // Mini-previews
  const [templatePreviews, setTemplatePreviews] = useState<Record<string, string>>({});
  const [loadingPreviews, setLoadingPreviews] = useState(true);

  // Download
  const [generating, setGenerating] = useState(false);

  // PDF viewer
  const [pdfViewerUri, setPdfViewerUri] = useState<string | null>(null);
  const [loadingPdf, setLoadingPdf] = useState(false);

  // Saved resumes
  const [savedResumes, setSavedResumes] = useState<SavedResumeInfo[]>([]);
  const [loadingSaved, setLoadingSaved] = useState(true);

  // AI warm-up state
  const [aiReady, setAiReady] = useState(false);
  const [aiWarmingUp, setAiWarmingUp] = useState(true);

  const loadSavedResumes = useCallback(async () => {
    try {
      const list = await exportService.listSaved();
      setSavedResumes(list);
    } catch {}
    setLoadingSaved(false);
  }, []);

  useEffect(() => { loadSavedResumes(); }, [loadSavedResumes]);

  // ── Pre-warm AI cache on mount ──
  useEffect(() => {
    let cancelled = false;
    (async () => {
      // Check if AI is already cached
      const ready = await exportService.isAiReady(portfolioId);
      if (ready) {
        setAiReady(true);
        setAiWarmingUp(false);
        return;
      }
      // Not cached — trigger warm-up in background
      setAiWarmingUp(true);
      exportService.warmUpAi(portfolioId);

      // Poll every 4s until AI is ready (max 120s)
      const maxAttempts = 30;
      for (let i = 0; i < maxAttempts && !cancelled; i++) {
        await new Promise<void>(r => setTimeout(() => r(), 4000));
        if (cancelled) break;
        const isReady = await exportService.isAiReady(portfolioId);
        if (isReady) {
          setAiReady(true);
          setAiWarmingUp(false);
          return;
        }
      }
      // Timed out but still usable — mark as done anyway
      if (!cancelled) {
        setAiReady(true);
        setAiWarmingUp(false);
      }
    })();
    return () => { cancelled = true; };
  }, [portfolioId]);

  const buildOptions = useCallback(
    (templateKey?: Template): ResumeOptions => ({
      template: templateKey ?? selected,
      aiRewrite: true,
      includePhoto,
      photoUrl:
        includePhoto
          ? (photoSource === 'upload' && uploadedPhotoUri
              ? uploadedPhotoUri
              : githubAvatarUrl)
          : undefined,
      includePhone: phone.trim().length > 0,
      phone: phone.trim() || undefined,
      includeLinkedIn: linkedIn.trim().length > 0,
      linkedIn: linkedIn.trim() || undefined,
      includeWebsite: website.trim().length > 0,
      website: website.trim() || undefined,
    }),
    [selected, includePhoto, photoSource, uploadedPhotoUri, phone, linkedIn, website, githubAvatarUrl],
  );

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoadingPreviews(true);
      const previews: Record<string, string> = {};
      for (const t of TEMPLATES) {
        try {
          const html = await exportService.getPreviewHtml(portfolioId, {
            template: t.key,
            includePhoto: false,
          });
          if (!cancelled) previews[t.key] = html;
        } catch {}
      }
      if (!cancelled) {
        setTemplatePreviews(previews);
        setLoadingPreviews(false);
      }
    })();
    return () => { cancelled = true; };
  }, [portfolioId]);

  const handlePreview = useCallback(async () => {
    setViewMode('preview');
    setLoadingPreview(true);
    setPreviewHtml(null);
    try {
      const html = await exportService.getPreviewHtml(portfolioId, buildOptions());
      setPreviewHtml(html);
    } catch (e: any) {
      Alert.alert('Preview failed', e?.message ?? 'Could not load preview');
      setViewMode('settings');
    } finally {
      setLoadingPreview(false);
    }
  }, [portfolioId, buildOptions]);

  const handleDownloadShare = useCallback(async () => {
    setGenerating(true);
    try {
      const saved = await exportService.generateAndSave(portfolioId, buildOptions());
      loadSavedResumes();
      Alert.alert('✅ Resume Saved!', `Your resume has been saved. You can find it in "My Resumes" anytime.`);
    } catch (e: any) {
      Alert.alert('Export failed', e?.message ?? 'Could not generate PDF');
    } finally {
      setGenerating(false);
    }
  }, [portfolioId, buildOptions, loadSavedResumes]);

  const handleShareSaved = useCallback(async (id: number) => {
    try {
      const b64 = await exportService.getSavedBase64(id);
      const filePath = `${RNFS.CachesDirectoryPath}/resume_${id}.pdf`;
      await RNFS.writeFile(filePath, b64, 'base64');
      await Share.share({url: `file://${filePath}`, title: 'My Resume'});
    } catch (e: any) {
      Alert.alert('Share failed', e?.message ?? 'Could not share PDF');
    }
  }, []);

  const handleOpenSaved = useCallback(async (id: number) => {
    setLoadingPdf(true);
    try {
      const b64 = await exportService.getSavedBase64(id);
      const filePath = `${RNFS.CachesDirectoryPath}/resume_${id}.pdf`;
      await RNFS.writeFile(filePath, b64, 'base64');
      setPdfViewerUri(filePath);
      setViewMode('pdfViewer');
    } catch (e: any) {
      Alert.alert('Error', e?.message ?? 'Could not open PDF');
    } finally {
      setLoadingPdf(false);
    }
  }, []);

  const handleDeleteSaved = useCallback(async (id: number) => {
    Alert.alert('Delete Resume', 'Are you sure you want to delete this saved resume?', [
      {text: 'Cancel', style: 'cancel'},
      {text: 'Delete', style: 'destructive', onPress: async () => {
        try {
          await exportService.deleteSaved(id);
          setSavedResumes(prev => prev.filter(r => r.id !== id));
        } catch {}
      }},
    ]);
  }, []);

  const handlePublishResume = useCallback(async (id: number) => {
    try {
      const updated = await exportService.publishResume(id);
      setSavedResumes(prev =>
        prev.map(r => (r.id === id ? {...r, publicUrl: updated.publicUrl} : r)),
      );
      if (updated.publicUrl) {
        Clipboard.setString(updated.publicUrl);
        Alert.alert(
          '🌐 Published!',
          'Your resume is live! The link has been copied to your clipboard.',
        );
      }
    } catch (e: any) {
      Alert.alert('Publish failed', e?.message ?? 'Something went wrong');
    }
  }, []);

  const handleUnpublishResume = useCallback(async (id: number) => {
    Alert.alert('Unpublish', 'Remove the public link?', [
      {text: 'Cancel', style: 'cancel'},
      {
        text: 'Unpublish',
        style: 'destructive',
        onPress: async () => {
          try {
            await exportService.unpublishResume(id);
            setSavedResumes(prev =>
              prev.map(r => (r.id === id ? {...r, publicUrl: null} : r)),
            );
          } catch (e: any) {
            Alert.alert('Error', e?.message ?? 'Could not unpublish');
          }
        },
      },
    ]);
  }, []);

  const handlePickPhoto = async () => {
    try {
      const result = await launchImageLibrary({
        mediaType: 'photo',
        quality: 0.8,
        maxWidth: 400,
        maxHeight: 400,
      });
      if (result.assets && result.assets[0]?.uri) {
        setUploadedPhotoUri(result.assets[0].uri);
        setPhotoSource('upload');
      }
    } catch {
      Alert.alert('Error', 'Could not open photo library');
    }
  };

  // ─── Preview View ──
  if (viewMode === 'preview') {
    return (
      <View style={[styles.container, {backgroundColor: '#1A1A1E'}]}>
        <StatusBar barStyle="light-content" />
        <Animated.View
          entering={FadeIn.duration(300)}
          style={[styles.header, {paddingTop: insets.top + 12, borderBottomColor: '#2A2A30'}]}>
          <TouchableOpacity onPress={() => setViewMode('settings')} activeOpacity={0.7}>
            <Text style={[styles.backText, {color: theme.colors.primary}]}>←</Text>
          </TouchableOpacity>
          <Text style={[styles.headerTitle, {color: '#fff'}]}>Preview</Text>
          <View style={{width: 32}} />
        </Animated.View>

        {loadingPreview ? (
          <Animated.View entering={FadeIn.duration(400)} style={{flex: 1}}>
            <AILoadingOverlay primaryColor={theme.colors.primary} />
          </Animated.View>
        ) : previewHtml ? (
          <>
            <Animated.View entering={FadeIn.delay(100).duration(400)} style={styles.previewPage}>
              <WebView
                source={{html: previewHtml}}
                style={styles.fullWebView}
                showsVerticalScrollIndicator={true}
                originWhitelist={['*']}
                javaScriptEnabled={true}
              />
            </Animated.View>
            <Animated.View
              entering={FadeInUp.duration(300).springify()}
              style={[styles.previewFooter, {
                paddingBottom: insets.bottom + 12,
                backgroundColor: '#0F0F12F5',
                borderTopColor: '#2A2A30',
              }]}>
              <TouchableOpacity
                onPress={() => setViewMode('settings')}
                style={[styles.footerBtn, {backgroundColor: '#1E1E24', borderColor: '#2E2E35'}]}>
                <Text style={styles.footerBtnIcon}>✏️</Text>
                <Text style={[styles.footerBtnText, {color: '#E5E7EB'}]}>Edit</Text>
              </TouchableOpacity>
              <TouchableOpacity
                onPress={handlePreview}
                style={[styles.footerBtn, {backgroundColor: '#1E1E24', borderColor: '#2E2E35'}]}>
                <Text style={styles.footerBtnIcon}>🔄</Text>
                <Text style={[styles.footerBtnText, {color: '#E5E7EB'}]}>Refresh</Text>
              </TouchableOpacity>
              <TouchableOpacity
                onPress={handleDownloadShare}
                disabled={generating}
                style={[styles.footerBtn, {
                  backgroundColor: theme.colors.primary,
                  borderColor: theme.colors.primary,
                  shadowColor: theme.colors.primary,
                  shadowOffset: {width: 0, height: 4},
                  shadowOpacity: 0.4,
                  shadowRadius: 10,
                }]}>
                {generating ? (
                  <View style={{flexDirection: 'row', alignItems: 'center', gap: 8}}>
                    <ActivityIndicator size="small" color="#fff" />
                    <Text style={[styles.footerBtnText, {color: '#fff'}]}>Generating...</Text>
                  </View>
                ) : (
                  <>
                    <Text style={styles.footerBtnIcon}>📥</Text>
                    <Text style={[styles.footerBtnText, {color: '#fff'}]}>Download</Text>
                  </>
                )}
              </TouchableOpacity>
            </Animated.View>
          </>
        ) : null}
      </View>
    );
  }

  // ─── PDF Viewer ──
  if (viewMode === 'pdfViewer' && pdfViewerUri) {
    return (
      <View style={[styles.container, {backgroundColor: '#1A1A1E'}]}>
        <StatusBar barStyle="light-content" />
        <Animated.View
          entering={FadeIn.duration(300)}
          style={[styles.header, {paddingTop: insets.top + 12, borderBottomColor: '#2A2A30'}]}>
          <TouchableOpacity
            onPress={() => { setViewMode('settings'); setPdfViewerUri(null); }}
            activeOpacity={0.7}>
            <Text style={[styles.backText, {color: theme.colors.primary}]}>←</Text>
          </TouchableOpacity>
          <Text style={[styles.headerTitle, {color: '#fff'}]}>Saved Resume</Text>
          <TouchableOpacity
            onPress={() => Share.share({url: `file://${pdfViewerUri}`, title: 'My Resume'})}
            activeOpacity={0.7}>
            <Text style={{fontSize: 20}}>📤</Text>
          </TouchableOpacity>
        </Animated.View>
        <WebView
          source={{uri: pdfViewerUri}}
          style={{flex: 1, backgroundColor: '#1A1A1E'}}
          originWhitelist={['file://*']}
        />
      </View>
    );
  }

  // ─── Settings View ──
  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <StatusBar barStyle="light-content" />

      <Animated.View
        entering={FadeIn.duration(300)}
        style={[styles.header, {paddingTop: insets.top + 12, borderBottomColor: theme.colors.border}]}>
        <TouchableOpacity onPress={() => navigation.goBack()} activeOpacity={0.7}>
          <Text style={[styles.backText, {color: theme.colors.primary}]}>←</Text>
        </TouchableOpacity>
        <View style={{flex: 1}}>
          <Text style={[styles.headerTitle, {color: theme.colors.text}]}>Build Resume</Text>
          {portfolio && (
            <Text style={[styles.headerSub, {color: theme.colors.textSecondary}]} numberOfLines={1}>
              {portfolio.title}
            </Text>
          )}
        </View>
        <View style={{width: 32}} />
      </Animated.View>

      <ScrollView
        contentContainerStyle={[styles.content, {paddingBottom: insets.bottom + 40}]}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled">

        {/* ── AI Warm-up Banner ── */}
        {aiWarmingUp && (
          <Animated.View
            entering={FadeInDown.duration(400).springify()}
            style={{
              flexDirection: 'row',
              alignItems: 'center',
              gap: 12,
              padding: 14,
              marginBottom: 16,
              borderRadius: 14,
              backgroundColor: theme.colors.primary + '15',
              borderWidth: 1,
              borderColor: theme.colors.primary + '30',
            }}>
            <ActivityIndicator size="small" color={theme.colors.primary} />
            <View style={{flex: 1}}>
              <Text style={{fontSize: 13, fontWeight: '700', color: theme.colors.text}}>
                🧠 AI is preparing your content...
              </Text>
              <Text style={{fontSize: 11, color: theme.colors.textSecondary, marginTop: 2}}>
                Enhancing descriptions & summary in background
              </Text>
            </View>
          </Animated.View>
        )}
        {aiReady && !aiWarmingUp && (
          <Animated.View
            entering={FadeInDown.duration(400).springify()}
            style={{
              flexDirection: 'row',
              alignItems: 'center',
              gap: 10,
              padding: 12,
              marginBottom: 16,
              borderRadius: 12,
              backgroundColor: '#10B98120',
              borderWidth: 1,
              borderColor: '#10B98140',
            }}>
            <Text style={{fontSize: 15}}>✅</Text>
            <Text style={{fontSize: 12, fontWeight: '600', color: '#10B981'}}>
              AI content ready — previews will load instantly
            </Text>
          </Animated.View>
        )}

        {/* ── Saved Resumes ── */}
        {(savedResumes.length > 0 || loadingSaved) && (
          <Animated.View entering={FadeInDown.delay(30).duration(400).springify()}>
            <Text style={[styles.sectionLabel, {color: theme.colors.textSecondary}]}>MY RESUMES</Text>
            {loadingSaved ? (
              <View style={{gap: 10}}>
                <Skeleton width={'100%'} height={72} borderRadius={14} color={theme.colors.border} />
                <Skeleton width={'100%'} height={72} borderRadius={14} color={theme.colors.border} />
              </View>
            ) : (
              savedResumes.map((sr, i) => {
                const tpl = TEMPLATES.find(t => t.key === sr.templateKey);
                const accent = tpl?.accent ?? theme.colors.primary;
                const tplName = tpl?.name ?? sr.templateKey;
                const sizeKB = Math.round(sr.fileSizeBytes / 1024);
                const dateStr = new Date(sr.createdAt).toLocaleDateString('en-US', {
                  month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
                });
                return (
                  <Animated.View
                    key={sr.id}
                    entering={FadeInDown.delay(i * 50 + 50).duration(350).springify()}>
                    <TouchableOpacity
                      onPress={() => handleOpenSaved(sr.id)}
                      activeOpacity={0.7}
                      style={[styles.savedCard, {
                        backgroundColor: theme.colors.card ?? theme.colors.surface,
                        borderColor: theme.colors.cardBorder ?? theme.colors.border,
                      }]}>
                      <View style={{flexDirection: 'row', alignItems: 'center', flex: 1, gap: 12}}>
                        <View style={[styles.savedIcon, {backgroundColor: accent + '18'}]}>
                          <Text style={{fontSize: 18}}>{tpl?.emoji ?? '📄'}</Text>
                        </View>
                        <View style={{flex: 1}}>
                          <Text style={[styles.savedTitle, {color: theme.colors.text}]} numberOfLines={1}>
                            {sr.title}
                          </Text>
                          <View style={{flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 3}}>
                            <View style={[styles.savedBadge, {backgroundColor: accent + '20', borderColor: accent + '40'}]}>
                              <View style={{width: 6, height: 6, borderRadius: 3, backgroundColor: accent}} />
                              <Text style={[styles.savedBadgeText, {color: accent}]}>{tplName}</Text>
                            </View>
                            <Text style={{color: theme.colors.textSecondary, fontSize: 11}}>{sizeKB} KB</Text>
                            <Text style={{color: theme.colors.textSecondary, fontSize: 11}}>•</Text>
                            <Text style={{color: theme.colors.textSecondary, fontSize: 11}}>{dateStr}</Text>
                          </View>
                        </View>
                      </View>
                      <View style={{flexDirection: 'row', gap: 6, marginTop: 10}}>
                        <TouchableOpacity
                          onPress={() => sr.publicUrl
                            ? handleUnpublishResume(sr.id)
                            : handlePublishResume(sr.id)}
                          activeOpacity={0.7}
                          style={[styles.savedActionBtn, sr.publicUrl
                            ? {backgroundColor: '#22C55E12', borderColor: '#22C55E30'}
                            : {backgroundColor: '#3B82F612', borderColor: '#3B82F630'}
                          ]}>
                          <Text style={{fontSize: 13}}>{sr.publicUrl ? '🌐' : '🌐'}</Text>
                          <Text style={[styles.savedActionText, {color: sr.publicUrl ? '#22C55E' : '#3B82F6'}]}>
                            {sr.publicUrl ? 'Live' : 'Publish'}
                          </Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                          onPress={() => navigation.navigate('ResumeEditor', {portfolioId})}
                          activeOpacity={0.7}
                          style={[styles.savedActionBtn, {backgroundColor: '#8B5CF612', borderColor: '#8B5CF630'}]}>
                          <Text style={{fontSize: 13}}>✏️</Text>
                          <Text style={[styles.savedActionText, {color: '#8B5CF6'}]}>Edit</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                          onPress={() => handleShareSaved(sr.id)}
                          activeOpacity={0.7}
                          style={[styles.savedActionBtn, {backgroundColor: theme.colors.primary + '12', borderColor: theme.colors.primary + '30'}]}>
                          <Text style={{fontSize: 13}}>📤</Text>
                          <Text style={[styles.savedActionText, {color: theme.colors.primary}]}>Share</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                          onPress={() => handleDeleteSaved(sr.id)}
                          activeOpacity={0.7}
                          style={[styles.savedActionBtn, {backgroundColor: '#EF444410', borderColor: '#EF444430'}]}>
                          <Text style={{fontSize: 13}}>🗑️</Text>
                          <Text style={[styles.savedActionText, {color: '#EF4444'}]}>Delete</Text>
                        </TouchableOpacity>
                      </View>
                      {sr.publicUrl && (
                        <TouchableOpacity
                          onPress={() => {
                            Clipboard.setString(sr.publicUrl!);
                            Alert.alert('Copied!', 'Public link copied to clipboard.');
                          }}
                          activeOpacity={0.7}
                          style={{
                            flexDirection: 'row',
                            alignItems: 'center',
                            gap: 8,
                            marginTop: 8,
                            padding: 10,
                            borderRadius: 10,
                            backgroundColor: '#22C55E08',
                            borderWidth: 1,
                            borderColor: '#22C55E20',
                          }}>
                          <View style={{
                            width: 24, height: 24, borderRadius: 6,
                            backgroundColor: '#22C55E15',
                            alignItems: 'center', justifyContent: 'center',
                          }}>
                            <Text style={{fontSize: 11}}>🔗</Text>
                          </View>
                          <Text
                            style={{flex: 1, fontSize: 11, color: '#A1A1AA', fontFamily: 'Courier New'}}
                            numberOfLines={1}
                            ellipsizeMode="middle">
                            {sr.publicUrl}
                          </Text>
                          <Text style={{fontSize: 11, fontWeight: '700', color: '#22C55E'}}>📋 Copy</Text>
                        </TouchableOpacity>
                      )}
                    </TouchableOpacity>
                  </Animated.View>
                );
              })
            )}
          </Animated.View>
        )}

        {/* ── Template Selection ── */}
        <Animated.View entering={FadeInDown.delay(50).duration(400).springify()}>
          <Text style={[styles.sectionLabel, {color: theme.colors.textSecondary}]}>CHOOSE TEMPLATE</Text>
          <View style={styles.templateGrid}>
            {[0, 2].map(rowStart => (
              <View key={rowStart} style={styles.templateRow}>
                {TEMPLATES.slice(rowStart, rowStart + 2).map((t, i) => {
                  const idx = rowStart + i;
                  const isActive = selected === t.key;
                  const html = templatePreviews[t.key];
                  return (
                    <Animated.View
                      key={t.key}
                      style={styles.templateCardWrapper}
                      entering={FadeInDown.delay(idx * 60 + 80).duration(400).springify().damping(16)}>
                      <TouchableOpacity
                        onPress={() => { setSelected(t.key); setPreviewHtml(null); }}
                        activeOpacity={0.8}
                        style={[styles.templateCard, {
                          backgroundColor: isActive ? t.accent + '12' : theme.colors.card ?? theme.colors.surface,
                          borderColor: isActive ? t.accent : theme.colors.cardBorder ?? theme.colors.border,
                          borderWidth: isActive ? 2 : 1,
                          shadowColor: isActive ? t.accent : 'transparent',
                          shadowOffset: {width: 0, height: isActive ? 4 : 0},
                          shadowOpacity: isActive ? 0.15 : 0,
                          shadowRadius: isActive ? 12 : 0,
                        }]}>
                        <View style={[styles.miniPreview, {backgroundColor: t.key === 'dark' ? '#0F0F14' : t.key === 'hacker' ? '#000' : '#fff'}]}>
                          {html ? (
                            <View style={{flex: 1, pointerEvents: 'none'}}>
                              <WebView
                                source={{html}}
                                style={{flex: 1, backgroundColor: 'transparent'}}
                                scrollEnabled={false}
                                showsVerticalScrollIndicator={false}
                                showsHorizontalScrollIndicator={false}
                                scalesPageToFit={false}
                                injectedJavaScript="document.body.style.zoom='0.22';document.body.style.overflow='hidden';true;"
                                pointerEvents="none"
                              />
                            </View>
                          ) : (
                            <View style={styles.previewPlaceholder}>
                              {loadingPreviews ? (
                                <Skeleton width={50} height={50} borderRadius={12} color={theme.colors.border} />
                              ) : (
                                <Text style={{fontSize: 28}}>{t.emoji}</Text>
                              )}
                            </View>
                          )}
                          {isActive && (
                            <View style={[styles.activeCheck, {backgroundColor: t.accent}]}>
                              <Text style={{color: '#fff', fontSize: 10, fontWeight: '800'}}>✓</Text>
                            </View>
                          )}
                        </View>
                        <View style={styles.templateMeta}>
                          <Text style={[styles.templateName, {color: theme.colors.text}]}>{t.name}</Text>
                          <Text style={[styles.templateDesc, {color: theme.colors.textSecondary}]}>{t.desc}</Text>
                        </View>
                      </TouchableOpacity>
                    </Animated.View>
                  );
                })}
              </View>
            ))}
          </View>
        </Animated.View>

        {/* ── Profile Photo ── */}
        <Animated.View entering={FadeInDown.delay(200).duration(400).springify()}>
          <Text style={[styles.sectionLabel, {color: theme.colors.textSecondary}]}>PROFILE PHOTO</Text>
          <View style={[styles.optionCard, {backgroundColor: theme.colors.card ?? theme.colors.surface, borderColor: theme.colors.cardBorder ?? theme.colors.border}]}>
            <View style={styles.optionRow}>
              <View style={[styles.optionIcon, {backgroundColor: theme.colors.primary + '12'}]}>
                <Text style={{fontSize: 16}}>📷</Text>
              </View>
              <Text style={[styles.optionLabel, {color: theme.colors.text, flex: 1}]}>Include photo</Text>
              <TouchableOpacity
                onPress={() => setIncludePhoto(v => !v)}
                activeOpacity={0.7}
                style={[styles.toggle, {
                  backgroundColor: includePhoto ? theme.colors.primary : theme.colors.border,
                }]}>
                <View style={[styles.toggleThumb, {
                  transform: [{translateX: includePhoto ? 20 : 2}],
                  backgroundColor: '#fff',
                }]} />
              </TouchableOpacity>
            </View>
            {includePhoto && (
              <Animated.View entering={FadeInDown.duration(250)} style={styles.photoOpts}>
                <TouchableOpacity
                  onPress={() => setPhotoSource('github')}
                  activeOpacity={0.7}
                  style={[styles.photoChip, {
                    backgroundColor: photoSource === 'github' ? theme.colors.primary + '15' : 'transparent',
                    borderColor: photoSource === 'github' ? theme.colors.primary : theme.colors.borderLight ?? theme.colors.border,
                  }]}>
                  <Text style={{fontSize: 18}}>🐙</Text>
                  <Text style={[styles.photoChipText, {color: photoSource === 'github' ? theme.colors.primary : theme.colors.textSecondary}]}>GitHub Avatar</Text>
                  {photoSource === 'github' && <Text style={{color: theme.colors.primary, fontWeight: '800'}}>✓</Text>}
                </TouchableOpacity>
                <TouchableOpacity
                  onPress={handlePickPhoto}
                  activeOpacity={0.7}
                  style={[styles.photoChip, {
                    backgroundColor: photoSource === 'upload' ? theme.colors.primary + '15' : 'transparent',
                    borderColor: photoSource === 'upload' ? theme.colors.primary : theme.colors.borderLight ?? theme.colors.border,
                  }]}>
                  {uploadedPhotoUri ? (
                    <Image source={{uri: uploadedPhotoUri}} style={styles.uploadThumb} />
                  ) : (
                    <Text style={{fontSize: 18}}>📁</Text>
                  )}
                  <Text style={[styles.photoChipText, {color: photoSource === 'upload' ? theme.colors.primary : theme.colors.textSecondary}]}>Upload</Text>
                  {photoSource === 'upload' && uploadedPhotoUri && <Text style={{color: theme.colors.primary, fontWeight: '800'}}>✓</Text>}
                </TouchableOpacity>
              </Animated.View>
            )}
          </View>
        </Animated.View>

        {/* ── Contact Details ── */}
        <Animated.View entering={FadeInDown.delay(280).duration(400).springify()}>
          <Text style={[styles.sectionLabel, {color: theme.colors.textSecondary}]}>CONTACT DETAILS</Text>
          <View style={[styles.optionCard, {backgroundColor: theme.colors.card ?? theme.colors.surface, borderColor: theme.colors.cardBorder ?? theme.colors.border}]}>
            {[
              {icon: '📱', label: 'Phone', value: phone, setter: setPhone, placeholder: '+1 (555) 123-4567', kbd: 'phone-pad' as const},
              {icon: '💼', label: 'LinkedIn', value: linkedIn, setter: setLinkedIn, placeholder: 'linkedin.com/in/username', kbd: 'url' as const},
              {icon: '🌐', label: 'Website', value: website, setter: setWebsite, placeholder: 'yoursite.com', kbd: 'url' as const},
            ].map((f, i) => (
              <View key={f.label} style={[styles.inputGroup, i > 0 && {borderTopWidth: 1, borderTopColor: theme.colors.borderLight ?? theme.colors.border, paddingTop: 12, marginTop: 12}]}>
                <View style={styles.inputLabelRow}>
                  <Text style={{fontSize: 13}}>{f.icon}</Text>
                  <Text style={[styles.inputLabel, {color: theme.colors.textSecondary}]}>{f.label}</Text>
                </View>
                <TextInput
                  value={f.value}
                  onChangeText={f.setter}
                  placeholder={f.placeholder}
                  placeholderTextColor={theme.colors.textMuted ?? '#52525B'}
                  keyboardType={f.kbd}
                  autoCapitalize="none"
                  style={[styles.input, {
                    color: theme.colors.text,
                    backgroundColor: theme.colors.surfaceElevated ?? theme.colors.surface,
                    borderColor: theme.colors.borderLight ?? theme.colors.border,
                  }]}
                />
              </View>
            ))}
          </View>
        </Animated.View>

        {/* ── Resume Sections Info ── */}
        <Animated.View entering={FadeInDown.delay(360).duration(400).springify()}>
          <Text style={[styles.sectionLabel, {color: theme.colors.textSecondary}]}>INCLUDED SECTIONS</Text>
          <View style={[styles.optionCard, {backgroundColor: theme.colors.card ?? theme.colors.surface, borderColor: theme.colors.cardBorder ?? theme.colors.border}]}>
            {[
              {emoji: '👤', text: 'Contact header with name, email, GitHub'},
              {emoji: '✍️', text: 'Professional summary (AI-generated)'},
              {emoji: '🛠', text: 'Technical skills grouped by proficiency'},
              {emoji: '💼', text: 'Work experience (if added)'},
              {emoji: '📁', text: 'Top projects with descriptions & stats'},
              {emoji: '🎓', text: 'Education (if added)'},
            ].map((item, i, arr) => (
              <View key={i} style={[styles.includedRow, i > 0 && {borderTopWidth: 1, borderTopColor: theme.colors.borderLight ?? theme.colors.border}]}>
                <Text style={{fontSize: 15}}>{item.emoji}</Text>
                <Text style={[styles.includedText, {color: theme.colors.text}]}>{item.text}</Text>
              </View>
            ))}
          </View>
        </Animated.View>

        {/* ── Edit Resume Sections ── */}
        <Animated.View entering={FadeInDown.delay(420).duration(400).springify()}>
          <TouchableOpacity
            onPress={() => navigation.navigate('ResumeEditor', {portfolioId})}
            activeOpacity={0.8}
            style={[styles.editSectionsBtn, {
              borderColor: theme.colors.primary + '40',
              backgroundColor: theme.colors.primary + '08',
            }]}>
            <Text style={{fontSize: 16}}>✏️</Text>
            <View style={{flex: 1}}>
              <Text style={[styles.editSectionsBtnTitle, {color: theme.colors.primary}]}>Edit Resume Sections</Text>
              <Text style={[styles.editSectionsBtnSub, {color: theme.colors.textSecondary}]}>Add education, experience & certifications</Text>
            </View>
            <Text style={{color: theme.colors.textSecondary, fontSize: 18}}>›</Text>
          </TouchableOpacity>
        </Animated.View>

        {/* ── Preview Button ── */}
        <Animated.View entering={FadeInDown.delay(440).duration(400).springify()}>
          <TouchableOpacity
            onPress={handlePreview}
            activeOpacity={0.8}
            style={[styles.previewBtn, {
              backgroundColor: theme.colors.primary,
              shadowColor: theme.colors.shadowColor ?? theme.colors.primary,
              shadowOffset: {width: 0, height: 6},
              shadowOpacity: 0.35,
              shadowRadius: 14,
              elevation: 6,
            }]}>
            <Text style={{fontSize: 18}}>👁</Text>
            <Text style={styles.previewBtnText}>Preview Resume</Text>
          </TouchableOpacity>
        </Animated.View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1},

  // Header
  header: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: 20,
    paddingBottom: 14,
    borderBottomWidth: 1,
    gap: 12,
  },
  backText: {fontSize: 26, fontWeight: '300', lineHeight: 30},
  headerTitle: {fontSize: 20, fontWeight: '800', letterSpacing: -0.5},
  headerSub: {fontSize: 12, fontWeight: '500', marginTop: 1},

  // Content
  content: {paddingHorizontal: 20, paddingTop: 16, gap: 18},

  // Section labels
  sectionLabel: {fontSize: 11, fontWeight: '700', letterSpacing: 1.5, marginBottom: 8},

  // Template grid
  templateGrid: {gap: 12},
  templateRow: {flexDirection: 'row', gap: 12},
  templateCardWrapper: {flex: 1},
  templateCard: {borderRadius: 14, overflow: 'hidden'},
  miniPreview: {
    height: 160,
    overflow: 'hidden',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(128,128,128,0.1)',
  },
  previewPlaceholder: {flex: 1, alignItems: 'center', justifyContent: 'center'},
  activeCheck: {
    position: 'absolute',
    top: 6,
    right: 6,
    width: 20,
    height: 20,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  templateMeta: {padding: 12, gap: 2},
  templateName: {fontSize: 15, fontWeight: '700'},
  templateDesc: {fontSize: 11, lineHeight: 16},

  // Option cards
  optionCard: {borderRadius: 14, borderWidth: 1, padding: 14},
  optionRow: {flexDirection: 'row', alignItems: 'center', gap: 12},
  optionIcon: {width: 36, height: 36, borderRadius: 10, alignItems: 'center', justifyContent: 'center'},
  optionLabel: {fontSize: 15, fontWeight: '700'},
  optionHint: {fontSize: 11, lineHeight: 16, marginTop: 2},

  // Toggle
  toggle: {width: 44, height: 24, borderRadius: 12, justifyContent: 'center'},
  toggleThumb: {width: 20, height: 20, borderRadius: 10, shadowColor: '#000', shadowOffset: {width: 0, height: 1}, shadowOpacity: 0.2, shadowRadius: 2},

  // Photo options
  photoOpts: {flexDirection: 'row', gap: 8, marginTop: 14},
  photoChip: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 10,
    borderWidth: 1,
  },
  photoChipText: {fontSize: 12, fontWeight: '600', flex: 1},
  uploadThumb: {width: 24, height: 24, borderRadius: 12},

  // Inputs
  inputGroup: {},
  inputLabelRow: {flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 6},
  inputLabel: {fontSize: 12, fontWeight: '600', letterSpacing: 0.3},
  input: {height: 44, borderWidth: 1, borderRadius: 10, paddingHorizontal: 12, fontSize: 14},

  // Included sections
  includedRow: {flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 10},
  includedText: {fontSize: 13, flex: 1},

  // Edit sections button
  editSectionsBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 14,
    borderWidth: 1,
    gap: 12,
  },
  editSectionsBtnTitle: {fontSize: 15, fontWeight: '700'},
  editSectionsBtnSub: {fontSize: 12, marginTop: 2},

  // Preview button
  previewBtn: {
    height: 56,
    borderRadius: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  previewBtnText: {color: '#fff', fontSize: 17, fontWeight: '800'},

  // Preview mode
  previewPage: {flex: 1, backgroundColor: '#2A2A30', padding: 8, borderRadius: 4},
  fullWebView: {flex: 1, borderRadius: 4, overflow: 'hidden'},
  previewFooter: {
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

  // Saved resumes
  savedCard: {
    borderWidth: 1,
    borderRadius: 14,
    padding: 14,
    marginBottom: 10,
  },
  savedIcon: {
    width: 40,
    height: 40,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  savedTitle: {
    fontSize: 15,
    fontWeight: '700',
    letterSpacing: 0.2,
  },
  savedBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: 6,
    borderWidth: 1,
  },
  savedBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.3,
  },
  savedActionBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 5,
    paddingVertical: 8,
    borderRadius: 10,
    borderWidth: 1,
  },
  savedActionText: {
    fontSize: 12,
    fontWeight: '700',
  },
});

