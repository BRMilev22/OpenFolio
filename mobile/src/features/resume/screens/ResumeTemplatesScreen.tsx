import React, {useEffect, useState, useCallback} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  StatusBar,
  Alert,
  Dimensions,
} from 'react-native';
import Animated, {FadeIn, FadeInDown, FadeInUp} from 'react-native-reanimated';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {WebView} from 'react-native-webview';
import type {AppScreenProps} from '../../../navigation/types';
import {useTheme} from '../../../theme';
import {resumeService} from '../services/resumeService';
import {Skeleton, PulseView} from '../../../components/AnimatedComponents';
import type {TemplateInfo, ResumeInfo} from '../../../api/types/resume';

type Props = AppScreenProps<'ResumeTemplates'>;

const CARD_MARGIN = 16;
const CARD_PADDING = 12;
const PREVIEW_ASPECT = 1.35;

export default function ResumeTemplatesScreen({route, navigation}: Props) {
  const {resumeId} = route.params;
  const {theme} = useTheme();
  const insets = useSafeAreaInsets();
  const screenWidth = Dimensions.get('window').width;

  const [templates, setTemplates] = useState<TemplateInfo[]>([]);
  const [resume, setResume] = useState<ResumeInfo | null>(null);
  const [previews, setPreviews] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [applying, setApplying] = useState<string | null>(null);

  const previewWidth = screenWidth - CARD_MARGIN * 2 - CARD_PADDING * 2;
  const previewHeight = previewWidth * PREVIEW_ASPECT;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [tpls, r] = await Promise.all([
        resumeService.getTemplates(),
        resumeService.get(resumeId),
      ]);
      setTemplates(tpls);
      setResume(r);

      const previewEntries = await Promise.all(
        tpls.map(async tpl => {
          try {
            const html = await resumeService.getTemplatePreviewHtml(resumeId, tpl.key);
            return [tpl.key, html] as [string, string];
          } catch {
            return [tpl.key, ''] as [string, string];
          }
        }),
      );
      setPreviews(Object.fromEntries(previewEntries));
    } catch (e: any) {
      Alert.alert('Error', e?.message ?? 'Failed to load templates');
    } finally {
      setLoading(false);
    }
  }, [resumeId]);

  useEffect(() => {
    load();
  }, [load]);

  const handleApply = async (templateKey: string) => {
    if (applying) return;
    setApplying(templateKey);
    try {
      const updated = await resumeService.update(resumeId, {templateKey});
      setResume(updated);
      Alert.alert('✅ Applied!', 'Template changed successfully.', [
        {
          text: 'Preview',
          onPress: () => navigation.navigate('ResumePreview', {resumeId}),
        },
        {text: 'OK', style: 'cancel'},
      ]);
    } catch (e: any) {
      Alert.alert('Error', e?.message ?? 'Failed to apply template');
    } finally {
      setApplying(null);
    }
  };

  if (loading) {
    return (
      <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
        <StatusBar barStyle="light-content" />
        <View style={[styles.header, {paddingTop: insets.top + 12, borderBottomColor: theme.colors.border}]}>
          <TouchableOpacity onPress={() => navigation.goBack()} activeOpacity={0.7}>
            <Text style={[styles.backText, {color: theme.colors.primary}]}>←</Text>
          </TouchableOpacity>
          <Text style={[styles.headerTitle, {color: theme.colors.text}]}>Choose Template</Text>
          <View style={{width: 32}} />
        </View>
        <View style={styles.loadingContent}>
          {[0, 1].map(i => (
            <Animated.View key={i} entering={FadeIn.delay(i * 100).duration(400)} style={[styles.skeletonCard, {backgroundColor: theme.colors.card ?? theme.colors.surface, borderColor: theme.colors.cardBorder ?? theme.colors.border}]}>
              <Skeleton width="100%" height={previewHeight * 0.6} borderRadius={10} color={theme.colors.border} />
              <View style={{marginTop: 12, gap: 6}}>
                <Skeleton width="50%" height={14} color={theme.colors.border} />
                <Skeleton width="80%" height={10} color={theme.colors.border} />
              </View>
            </Animated.View>
          ))}
        </View>
      </View>
    );
  }

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
        <View style={{flex: 1}}>
          <Text style={[styles.headerTitle, {color: theme.colors.text}]}>Choose Template</Text>
          <Text style={[styles.headerSub, {color: theme.colors.textSecondary}]}>
            {templates.length} templates available
          </Text>
        </View>
        <View style={{width: 32}} />
      </Animated.View>

      <ScrollView
        contentContainerStyle={[styles.grid, {paddingBottom: insets.bottom + 32}]}
        showsVerticalScrollIndicator={false}>

        {templates.map((tpl, index) => {
          const isActive = resume?.templateKey === tpl.key;
          const previewHtml = previews[tpl.key];

          return (
            <Animated.View
              key={tpl.key}
              entering={FadeInDown.delay(index * 100 + 100).duration(500).springify().damping(16)}>
              <View style={[styles.templateCard, {
                backgroundColor: theme.colors.card ?? theme.colors.surface,
                borderColor: isActive ? theme.colors.primary : theme.colors.cardBorder ?? theme.colors.border,
                borderWidth: isActive ? 2.5 : 1,
                shadowColor: isActive ? theme.colors.primary : 'transparent',
                shadowOffset: {width: 0, height: isActive ? 8 : 0},
                shadowOpacity: isActive ? 0.2 : 0,
                shadowRadius: isActive ? 20 : 0,
              }]}>
                {/* Active badge */}
                {isActive && (
                  <Animated.View
                    entering={FadeIn.duration(300)}
                    style={[styles.activeBadge, {backgroundColor: theme.colors.primary}]}>
                    <Text style={styles.activeBadgeText}>✓ Active</Text>
                  </Animated.View>
                )}

                {/* Preview */}
                <TouchableOpacity
                  activeOpacity={0.9}
                  onPress={() => handleApply(tpl.key)}
                  disabled={!!applying}
                  style={[styles.previewContainer, {height: previewHeight, borderColor: theme.colors.borderLight ?? theme.colors.border}]}>
                  {previewHtml ? (
                    <WebView
                      originWhitelist={['*']}
                      source={{html: previewHtml}}
                      style={styles.previewWebView}
                      scrollEnabled={false}
                      bounces={false}
                      scalesPageToFit={true}
                      javaScriptEnabled={false}
                      showsVerticalScrollIndicator={false}
                      showsHorizontalScrollIndicator={false}
                      pointerEvents="none"
                    />
                  ) : (
                    <View style={styles.centered}>
                      <PulseView>
                        <ActivityIndicator size="small" color={theme.colors.primary} />
                      </PulseView>
                    </View>
                  )}
                </TouchableOpacity>

                {/* Template info */}
                <View style={styles.templateInfo}>
                  <View style={styles.templateInfoRow}>
                    <View style={{flexDirection: 'row', alignItems: 'center', gap: 8, flex: 1}}>
                      <View style={[styles.emojiWrap, {backgroundColor: theme.colors.primary + '12'}]}>
                        <Text style={{fontSize: 20}}>{tpl.emoji ?? '📄'}</Text>
                      </View>
                      <View style={{flex: 1}}>
                        <Text style={[styles.templateName, {color: theme.colors.text}]}>{tpl.name}</Text>
                        <Text style={[styles.templateDesc, {color: theme.colors.textSecondary}]} numberOfLines={2}>
                          {tpl.description}
                        </Text>
                      </View>
                    </View>

                    {!isActive ? (
                      <TouchableOpacity
                        onPress={() => handleApply(tpl.key)}
                        disabled={applying === tpl.key}
                        activeOpacity={0.7}
                        style={[styles.applyBtn, {
                          backgroundColor: theme.colors.primary,
                          shadowColor: theme.colors.primary,
                          shadowOffset: {width: 0, height: 3},
                          shadowOpacity: 0.3,
                          shadowRadius: 6,
                          opacity: applying === tpl.key ? 0.5 : 1,
                        }]}>
                        {applying === tpl.key ? (
                          <ActivityIndicator size="small" color="#fff" />
                        ) : (
                          <Text style={styles.applyBtnText}>Apply</Text>
                        )}
                      </TouchableOpacity>
                    ) : (
                      <View style={[styles.applyBtn, {
                        backgroundColor: theme.colors.primary + '12',
                        borderWidth: 1,
                        borderColor: theme.colors.primary + '30',
                      }]}>
                        <Text style={[styles.applyBtnText, {color: theme.colors.primary}]}>Active</Text>
                      </View>
                    )}
                  </View>
                </View>
              </View>
            </Animated.View>
          );
        })}
      </ScrollView>
    </View>
  );
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
  headerTitle: {fontSize: 20, fontWeight: '800', letterSpacing: -0.5},
  headerSub: {fontSize: 12, fontWeight: '500', marginTop: 1},

  // Loading
  loadingContent: {padding: 16, gap: 16},
  skeletonCard: {padding: 16, borderRadius: 16, borderWidth: 1},

  // Grid
  grid: {paddingHorizontal: CARD_MARGIN, paddingTop: 16, gap: 20},
  templateCard: {borderRadius: 18, overflow: 'hidden'},
  activeBadge: {
    position: 'absolute',
    top: 14,
    right: 14,
    zIndex: 10,
    paddingHorizontal: 12,
    paddingVertical: 5,
    borderRadius: 10,
  },
  activeBadgeText: {color: '#fff', fontSize: 11, fontWeight: '800'},
  previewContainer: {
    margin: CARD_PADDING,
    marginBottom: 0,
    borderRadius: 10,
    borderWidth: 1,
    overflow: 'hidden',
    backgroundColor: '#fff',
  },
  previewWebView: {flex: 1, backgroundColor: '#fff'},
  templateInfo: {padding: 14},
  templateInfoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  emojiWrap: {
    width: 40,
    height: 40,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  templateName: {fontSize: 16, fontWeight: '800'},
  templateDesc: {fontSize: 12, lineHeight: 17, marginTop: 2},
  applyBtn: {
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 10,
  },
  applyBtnText: {color: '#fff', fontSize: 13, fontWeight: '800'},
});
