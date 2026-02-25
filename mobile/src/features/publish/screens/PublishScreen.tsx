import React, {useState} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  Alert,
  Share,
  StatusBar,
} from 'react-native';
import Animated, {FadeIn, FadeInDown, FadeInUp} from 'react-native-reanimated';
import Clipboard from '@react-native-clipboard/clipboard';
import QRCode from 'react-native-qrcode-svg';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import type {AppScreenProps} from '../../../navigation/types';
import {useTheme} from '../../../theme';
import {usePortfolioStore} from '../../dashboard/store/portfolioStore';
import {publishService} from '../services/publishService';
import {PulseView} from '../../../components/AnimatedComponents';
import type {PublishResponse} from '../../../api/types/portfolio';

type Props = AppScreenProps<'Publish'>;

export default function PublishScreen({route, navigation}: Props) {
  const {portfolioId} = route.params;
  const {theme} = useTheme();
  const insets = useSafeAreaInsets();

  const portfolio = usePortfolioStore(s => s.portfolios.find(p => p.id === portfolioId));
  const fetchPortfolios = usePortfolioStore(s => s.fetchPortfolios);

  const [loading, setLoading] = useState(false);
  const [publishRecord, setPublishRecord] = useState<PublishResponse | null>(null);
  const [copied, setCopied] = useState(false);

  const publicUrl = publishRecord?.publicUrl
    ?? (portfolio?.published ? `http://localhost:8080/api/v1/public/${portfolio.slug}` : null);

  const handlePublish = async () => {
    setLoading(true);
    try {
      const record = await publishService.publish(portfolioId);
      setPublishRecord(record);
      await fetchPortfolios();
    } catch (e: any) {
      Alert.alert('Publish failed', e?.message ?? 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  const handleUnpublish = async () => {
    Alert.alert('Unpublish', 'This will take your portfolio offline. Continue?', [
      {text: 'Cancel', style: 'cancel'},
      {
        text: 'Unpublish',
        style: 'destructive',
        onPress: async () => {
          setLoading(true);
          try {
            await publishService.unpublish(portfolioId);
            setPublishRecord(null);
            await fetchPortfolios();
          } catch (e: any) {
            Alert.alert('Error', e?.message ?? 'Failed to unpublish');
          } finally {
            setLoading(false);
          }
        },
      },
    ]);
  };

  const handleShare = async () => {
    if (!publicUrl) return;
    await Share.share({
      message: `Check out my portfolio: ${publicUrl}`,
      url: publicUrl,
      title: portfolio?.title ?? 'My Portfolio',
    });
  };

  const handleCopy = () => {
    if (!publicUrl) return;
    Clipboard.setString(publicUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const isPublished = portfolio?.published || !!publishRecord;

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
        <Text style={[styles.headerTitle, {color: theme.colors.text}]}>Publish</Text>
        <View style={{width: 32}} />
      </Animated.View>

      <ScrollView
        contentContainerStyle={[styles.content, {paddingBottom: insets.bottom + 40}]}
        showsVerticalScrollIndicator={false}>

        {/* Status Card */}
        <Animated.View entering={FadeInDown.delay(80).duration(450).springify().damping(16)}>
          <View style={[styles.statusCard, {
            backgroundColor: isPublished ? '#22C55E08' : theme.colors.card ?? theme.colors.surface,
            borderColor: isPublished ? '#22C55E33' : theme.colors.cardBorder ?? theme.colors.border,
          }]}>
            <PulseView>
              <View style={[styles.statusIcon, {
                backgroundColor: isPublished ? '#22C55E15' : theme.colors.primary + '12',
                borderColor: isPublished ? '#22C55E25' : theme.colors.primary + '20',
                borderWidth: 1,
              }]}>
                <Text style={{fontSize: 26}}>
                  {isPublished ? '🌐' : '📝'}
                </Text>
              </View>
            </PulseView>
            <View style={styles.statusInfo}>
              <Text style={[styles.statusTitle, {color: theme.colors.text}]}>
                {isPublished ? 'Your portfolio is live' : 'Not published yet'}
              </Text>
              <Text style={[styles.statusSub, {color: theme.colors.textSecondary}]}>
                {isPublished
                  ? `Version ${publishRecord?.version ?? 1} · Visible to anyone with the link`
                  : 'Publish to share your portfolio with the world'}
              </Text>
            </View>
            {isPublished && (
              <View style={[styles.liveBadge, {backgroundColor: '#22C55E'}]}>
                <View style={{width: 5, height: 5, borderRadius: 3, backgroundColor: '#fff'}} />
                <Text style={styles.liveBadgeText}>Live</Text>
              </View>
            )}
          </View>
        </Animated.View>

        {/* QR Code */}
        {isPublished && publicUrl ? (
          <Animated.View entering={FadeInDown.delay(160).duration(450).springify()}>
            <View style={[styles.qrSection, {
              backgroundColor: theme.colors.card ?? theme.colors.surface,
              borderColor: theme.colors.cardBorder ?? theme.colors.border,
            }]}>
              <Text style={[styles.sectionLabel, {color: theme.colors.textSecondary}]}>QR CODE</Text>
              <View style={[styles.qrWrapper, {
                shadowColor: theme.colors.primary,
                shadowOffset: {width: 0, height: 4},
                shadowOpacity: 0.1,
                shadowRadius: 12,
              }]}>
                <QRCode
                  value={publicUrl}
                  size={180}
                  color="#09090B"
                  backgroundColor="#FFFFFF"
                />
              </View>
              <Text style={[styles.qrHint, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
                Scan to open your portfolio
              </Text>
            </View>
          </Animated.View>
        ) : null}

        {/* URL row */}
        {isPublished && publicUrl ? (
          <Animated.View entering={FadeInDown.delay(240).duration(450).springify()}>
            <View style={[styles.urlRow, {
              backgroundColor: theme.colors.card ?? theme.colors.surface,
              borderColor: theme.colors.cardBorder ?? theme.colors.border,
            }]}>
              <View style={[styles.urlIconWrap, {backgroundColor: theme.colors.primary + '12'}]}>
                <Text style={{fontSize: 14}}>🔗</Text>
              </View>
              <Text style={[styles.urlText, {color: theme.colors.textSecondary}]} numberOfLines={1} ellipsizeMode="middle">
                {publicUrl}
              </Text>
              <TouchableOpacity
                onPress={handleCopy}
                activeOpacity={0.7}
                style={[styles.copyBtn, {
                  backgroundColor: copied ? '#22C55E15' : theme.colors.primary + '15',
                  borderColor: copied ? '#22C55E30' : theme.colors.primary + '30',
                }]}>
                <Text style={[styles.copyBtnText, {color: copied ? '#22C55E' : theme.colors.primary}]}>
                  {copied ? '✓ Copied' : '📋 Copy'}
                </Text>
              </TouchableOpacity>
            </View>
          </Animated.View>
        ) : null}

        {/* Actions */}
        <Animated.View entering={FadeInDown.delay(320).duration(450).springify()}>
          <View style={styles.actions}>
            {isPublished ? (
              <>
                <TouchableOpacity
                  onPress={handleShare}
                  activeOpacity={0.8}
                  style={[styles.primaryBtn, {
                    backgroundColor: theme.colors.primary,
                    shadowColor: theme.colors.shadowColor ?? theme.colors.primary,
                    shadowOffset: {width: 0, height: 6},
                    shadowOpacity: 0.35,
                    shadowRadius: 14,
                    elevation: 6,
                  }]}>
                  <Text style={{fontSize: 16}}>📤</Text>
                  <Text style={styles.primaryBtnText}>Share Portfolio</Text>
                </TouchableOpacity>

                <TouchableOpacity
                  onPress={handlePublish}
                  activeOpacity={0.8}
                  style={[styles.secondaryBtn, {
                    backgroundColor: theme.colors.card ?? theme.colors.surface,
                    borderColor: theme.colors.cardBorder ?? theme.colors.border,
                  }]}>
                  {loading ? (
                    <ActivityIndicator size="small" color={theme.colors.primary} />
                  ) : (
                    <>
                      <Text style={{fontSize: 14}}>🔄</Text>
                      <Text style={[styles.secondaryBtnText, {color: theme.colors.text}]}>Re-publish (update)</Text>
                    </>
                  )}
                </TouchableOpacity>

                <TouchableOpacity
                  onPress={() => navigation.navigate('Export', {portfolioId})}
                  activeOpacity={0.8}
                  style={[styles.secondaryBtn, {
                    backgroundColor: theme.colors.card ?? theme.colors.surface,
                    borderColor: theme.colors.cardBorder ?? theme.colors.border,
                  }]}>
                  <Text style={{fontSize: 14}}>📄</Text>
                  <Text style={[styles.secondaryBtnText, {color: theme.colors.text}]}>Export as PDF</Text>
                </TouchableOpacity>

                <TouchableOpacity
                  onPress={handleUnpublish}
                  activeOpacity={0.8}
                  style={[styles.dangerBtn, {borderColor: '#F8717120'}]}>
                  <Text style={{fontSize: 14}}>⛔</Text>
                  <Text style={[styles.dangerBtnText, {color: '#F87171'}]}>Take Offline</Text>
                </TouchableOpacity>
              </>
            ) : (
              <TouchableOpacity
                onPress={handlePublish}
                activeOpacity={0.8}
                disabled={loading}
                style={[styles.primaryBtn, {
                  backgroundColor: theme.colors.primary,
                  shadowColor: theme.colors.shadowColor ?? theme.colors.primary,
                  shadowOffset: {width: 0, height: 6},
                  shadowOpacity: 0.35,
                  shadowRadius: 14,
                  opacity: loading ? 0.6 : 1,
                }]}>
                {loading ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <>
                    <Text style={{fontSize: 18}}>🚀</Text>
                    <Text style={styles.primaryBtnText}>Publish Portfolio</Text>
                  </>
                )}
              </TouchableOpacity>
            )}
          </View>
        </Animated.View>

        {/* Info */}
        <Animated.View entering={FadeInDown.delay(400).duration(450).springify()}>
          <View style={[styles.infoBox, {
            backgroundColor: theme.colors.primary + '06',
            borderColor: theme.colors.primary + '15',
          }]}>
            <Text style={[styles.infoTitle, {color: theme.colors.primary}]}>💡 How publishing works</Text>
            <Text style={[styles.infoText, {color: theme.colors.textSecondary}]}>
              Your portfolio gets a unique public URL based on your slug. Anyone with the link can view your projects, skills, and about section. You can unpublish at any time.
            </Text>
          </View>
        </Animated.View>
      </ScrollView>
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
  content: {paddingHorizontal: 20, paddingTop: 24, gap: 16},

  // Status
  statusCard: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 14,
    padding: 18,
    borderRadius: 18,
    borderWidth: 1,
  },
  statusIcon: {width: 52, height: 52, borderRadius: 16, alignItems: 'center', justifyContent: 'center'},
  statusInfo: {flex: 1, gap: 4},
  statusTitle: {fontSize: 17, fontWeight: '800'},
  statusSub: {fontSize: 13, lineHeight: 19},
  liveBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    position: 'absolute',
    top: 14,
    right: 14,
  },
  liveBadgeText: {color: '#fff', fontSize: 10, fontWeight: '800', letterSpacing: 0.5},

  // QR
  qrSection: {
    alignItems: 'center',
    padding: 24,
    borderRadius: 18,
    borderWidth: 1,
    gap: 14,
  },
  sectionLabel: {fontSize: 11, fontWeight: '700', letterSpacing: 1.5, alignSelf: 'flex-start'},
  qrWrapper: {padding: 16, borderRadius: 14, backgroundColor: '#fff'},
  qrHint: {fontSize: 12, fontWeight: '600'},

  // URL
  urlRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 12,
    borderRadius: 14,
    borderWidth: 1,
  },
  urlIconWrap: {width: 32, height: 32, borderRadius: 8, alignItems: 'center', justifyContent: 'center'},
  urlText: {flex: 1, fontSize: 12, fontFamily: 'Courier New'},
  copyBtn: {paddingHorizontal: 12, paddingVertical: 7, borderRadius: 8, borderWidth: 1},
  copyBtnText: {fontSize: 12, fontWeight: '700'},

  // Actions
  actions: {gap: 10},
  primaryBtn: {
    height: 56,
    borderRadius: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  primaryBtnText: {color: '#fff', fontSize: 17, fontWeight: '800'},
  secondaryBtn: {
    height: 52,
    borderRadius: 14,
    borderWidth: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  secondaryBtnText: {fontSize: 15, fontWeight: '700'},
  dangerBtn: {
    height: 48,
    borderRadius: 14,
    borderWidth: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
  },
  dangerBtnText: {fontSize: 14, fontWeight: '700'},

  // Info
  infoBox: {padding: 16, borderRadius: 16, borderWidth: 1, gap: 6},
  infoTitle: {fontSize: 14, fontWeight: '700'},
  infoText: {fontSize: 13, lineHeight: 20},
});

