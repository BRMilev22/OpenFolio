import React, {useEffect, useState, useCallback, useRef} from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  StatusBar,
  ActivityIndicator,
  Alert,
  RefreshControl,
  Pressable,
} from 'react-native';
import Animated, {
  FadeInDown,
  FadeIn,
  FadeInUp,
  useSharedValue,
  useAnimatedStyle,
  withSpring,
  withTiming,
  SlideInRight,
} from 'react-native-reanimated';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {useTheme} from '../../../theme';
import {useAuthStore} from '../../auth/store/authStore';
import {usePortfolioStore} from '../store/portfolioStore';
import {portfolioService} from '../services/portfolioService';
import {Card} from '../../../components/Card';
import {Button} from '../../../components/Button';
import {GlowBlob, PulseView, Skeleton} from '../../../components/AnimatedComponents';
import type {Portfolio} from '../../../api/types/portfolio';
import type {AppScreenProps} from '../../../navigation/types';

type Props = AppScreenProps<'Dashboard'>;

const SPRING = {damping: 15, stiffness: 180, mass: 0.7};

export default function DashboardScreen({navigation}: Props) {
  const {theme} = useTheme();
  const insets = useSafeAreaInsets();
  const displayName = useAuthStore(s => s.displayName);
  const githubUsername = useAuthStore(s => s.githubUsername);
  const logout = useAuthStore(s => s.logout);
  const portfolios = usePortfolioStore(s => s.portfolios);
  const loading = usePortfolioStore(s => s.loading);
  const error = usePortfolioStore(s => s.error);
  const fetchPortfolios = usePortfolioStore(s => s.fetchPortfolios);
  const removePortfolio = usePortfolioStore(s => s.removePortfolio);

  const [importing, setImporting] = useState(false);
  const autoImportDone = useRef(false);

  useEffect(() => {
    fetchPortfolios();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (
      !autoImportDone.current &&
      !loading &&
      portfolios.length === 0 &&
      githubUsername &&
      !importing
    ) {
      autoImportDone.current = true;
      handleImport();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, portfolios.length, githubUsername]);

  const handleSignOut = () => {
    Alert.alert('Sign Out', 'Are you sure you want to sign out?', [
      {text: 'Cancel', style: 'cancel'},
      {text: 'Sign Out', style: 'destructive', onPress: () => logout()},
    ]);
  };

  const handleDeletePortfolio = (portfolio: Portfolio) => {
    Alert.alert(
      'Delete Portfolio',
      `Delete "${portfolio.title}"? This cannot be undone.`,
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await portfolioService.delete(portfolio.id);
              removePortfolio(portfolio.id);
            } catch {
              Alert.alert('Error', 'Could not delete portfolio');
            }
          },
        },
      ],
    );
  };

  const handleImport = async () => {
    if (!githubUsername) return;
    setImporting(true);
    try {
      await portfolioService.ingestFromGitHub({githubUsername});
      // Refresh from server — backend deletes old portfolios on re-import,
      // so fetching ensures local state matches the single source of truth.
      await fetchPortfolios();
    } catch (e: unknown) {
      Alert.alert(
        'Import Failed',
        e instanceof Error ? e.message : 'Could not import from GitHub',
      );
    } finally {
      setImporting(false);
    }
  };

  const renderPortfolioCard = useCallback(
    ({item, index}: {item: Portfolio; index: number}) => (
      <Animated.View
        entering={FadeInDown.delay(index * 80 + 200)
          .duration(500)
          .springify()
          .damping(16)}>
        <Card
          style={styles.portfolioCard}
          onPress={() => navigation.navigate('Editor', {portfolioId: item.id})}
          elevated
          glowColor={item.published ? theme.colors.success : undefined}>

          {/* Top accent line */}
          <View style={[styles.accentLine, {backgroundColor: item.published ? theme.colors.success : theme.colors.primary}]} />

          <View style={styles.cardContent}>
            <View style={styles.cardHeader}>
              <View
                style={[
                  styles.statusPill,
                  {
                    backgroundColor: item.published
                      ? theme.colors.success + '18'
                      : theme.colors.primary + '12',
                  },
                ]}>
                <View
                  style={[
                    styles.statusDot,
                    {
                      backgroundColor: item.published
                        ? theme.colors.success
                        : theme.colors.textMuted ?? '#52525B',
                    },
                  ]}
                />
                <Text
                  style={[
                    styles.statusText,
                    {
                      color: item.published
                        ? theme.colors.success
                        : theme.colors.textMuted ?? '#52525B',
                    },
                  ]}>
                  {item.published ? 'Live' : 'Draft'}
                </Text>
              </View>
              <TouchableOpacity
                onPress={() => handleDeletePortfolio(item)}
                style={styles.deleteBtn}
                activeOpacity={0.7}>
                <Text style={{color: theme.colors.error + '88', fontSize: 14}}>🗑</Text>
              </TouchableOpacity>
            </View>

            <Text
              style={[styles.portfolioTitle, {color: theme.colors.text}]}
              numberOfLines={1}>
              {item.title}
            </Text>
            {item.tagline ? (
              <Text
                style={[styles.portfolioTagline, {color: theme.colors.textSecondary}]}
                numberOfLines={2}>
                {item.tagline}
              </Text>
            ) : null}

            {/* Stats row with visual polish */}
            <View style={[styles.statsRow, {borderTopColor: theme.colors.borderLight ?? theme.colors.border}]}>
              <View style={styles.stat}>
                <Text style={[styles.statValue, {color: theme.colors.primary}]}>
                  {item.projectCount}
                </Text>
                <Text style={[styles.statLabel, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
                  Projects
                </Text>
              </View>
              <View style={[styles.statDivider, {backgroundColor: theme.colors.borderLight ?? theme.colors.border}]} />
              <View style={styles.stat}>
                <Text style={[styles.statValue, {color: theme.colors.secondary ?? theme.colors.primary}]}>
                  {item.skillCount}
                </Text>
                <Text style={[styles.statLabel, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
                  Skills
                </Text>
              </View>
              <View style={[styles.statDivider, {backgroundColor: theme.colors.borderLight ?? theme.colors.border}]} />
              <View style={styles.stat}>
                <Text style={[styles.statValue, {color: theme.colors.accent ?? theme.colors.primary}]}>
                  →
                </Text>
                <Text style={[styles.statLabel, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
                  Open
                </Text>
              </View>
            </View>
          </View>
        </Card>
      </Animated.View>
    ),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [theme, navigation],
  );

  const renderEmpty = () => {
    if (loading || importing) return null;
    return (
      <Animated.View
        entering={FadeIn.delay(300).duration(600)}
        style={styles.emptyState}>
        <PulseView>
          <View
            style={[
              styles.emptyIcon,
              {
                backgroundColor: theme.colors.primary + '12',
                borderColor: theme.colors.primary + '25',
                shadowColor: theme.colors.primary,
                shadowOffset: {width: 0, height: 0},
                shadowOpacity: 0.2,
                shadowRadius: 20,
              },
            ]}>
            <Text style={{fontSize: 40}}>📁</Text>
          </View>
        </PulseView>
        <Text style={[styles.emptyTitle, {color: theme.colors.text}]}>
          No portfolios yet
        </Text>
        <Text style={[styles.emptySubtitle, {color: theme.colors.textSecondary}]}>
          Import your GitHub profile to generate a beautiful portfolio in seconds.
        </Text>
        <Button
          label="⚡ Generate Portfolio"
          onPress={handleImport}
          loading={importing}
          style={styles.emptyButton}
          fullWidth={false}
          size="lg"
        />
      </Animated.View>
    );
  };

  const renderLoadingSkeleton = () => (
    <Animated.View entering={FadeIn.duration(400)} style={styles.skeletonContainer}>
      {importing && (
        <Animated.View
          entering={FadeInDown.duration(400)}
          style={[styles.importingBanner, {backgroundColor: theme.colors.primary + '12', borderColor: theme.colors.primary + '25'}]}>
          <ActivityIndicator size="small" color={theme.colors.primary} />
          <Text style={[styles.importingText, {color: theme.colors.primary}]}>
            Building your portfolio from GitHub…
          </Text>
        </Animated.View>
      )}
      {[0, 1, 2].map(i => (
        <View
          key={i}
          style={[styles.skeletonCard, {
            backgroundColor: theme.colors.card ?? theme.colors.surface,
            borderColor: theme.colors.cardBorder ?? theme.colors.border,
          }]}>
          <Skeleton width="30%" height={12} color={theme.colors.border} />
          <Skeleton width="70%" height={18} color={theme.colors.border} style={{marginTop: 12}} />
          <Skeleton width="50%" height={12} color={theme.colors.border} style={{marginTop: 8}} />
          <View style={styles.skeletonStats}>
            <Skeleton width={60} height={28} borderRadius={6} color={theme.colors.border} />
            <Skeleton width={60} height={28} borderRadius={6} color={theme.colors.border} />
            <Skeleton width={60} height={28} borderRadius={6} color={theme.colors.border} />
          </View>
        </View>
      ))}
    </Animated.View>
  );

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <StatusBar barStyle="light-content" backgroundColor={theme.colors.background} />

      {/* Subtle glow behind header */}
      <GlowBlob
        color={theme.colors.primary}
        size={260}
        style={{top: -160, left: -60}}
      />

      {/* Header — animated entrance */}
      <Animated.View
        entering={FadeInDown.duration(400).springify().damping(20)}
        style={[styles.header, {paddingTop: insets.top + 12}]}>
        <View style={styles.headerLeft}>
          <Text style={[styles.greeting, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
            {getGreeting()}
          </Text>
          <Text style={[styles.userName, {color: theme.colors.text}]}>
            {displayName ?? githubUsername ?? 'Developer'}
          </Text>
        </View>
        <TouchableOpacity
          onPress={handleSignOut}
          style={[
            styles.avatarBtn,
            {
              backgroundColor: theme.colors.primary + '18',
              borderColor: theme.colors.primary + '33',
            },
          ]}
          activeOpacity={0.7}>
          <Text style={[styles.avatarText, {color: theme.colors.primary}]}>
            {(displayName ?? githubUsername ?? 'D')[0].toUpperCase()}
          </Text>
        </TouchableOpacity>
      </Animated.View>

      {/* Quick actions bar */}
      {portfolios.length > 0 && (
        <Animated.View
          entering={FadeInDown.delay(100).duration(400).springify()}
          style={[styles.quickActions, {borderBottomColor: theme.colors.borderLight ?? theme.colors.border}]}>
          <TouchableOpacity
            onPress={handleImport}
            disabled={importing}
            style={[styles.quickAction, {backgroundColor: theme.colors.primary + '10', borderColor: theme.colors.primary + '22'}]}
            activeOpacity={0.7}>
            <Text style={styles.quickActionIcon}>{importing ? '⏳' : '🔄'}</Text>
            <Text style={[styles.quickActionText, {color: theme.colors.primary}]}>
              {importing ? 'Importing…' : 'Re-import'}
            </Text>
          </TouchableOpacity>
          <View style={[styles.portfolioCount, {backgroundColor: theme.colors.card ?? theme.colors.surface, borderColor: theme.colors.cardBorder ?? theme.colors.border}]}>
            <Text style={[styles.portfolioCountNum, {color: theme.colors.text}]}>
              {portfolios.length}
            </Text>
            <Text style={[styles.portfolioCountLabel, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
              {portfolios.length === 1 ? 'Portfolio' : 'Portfolios'}
            </Text>
          </View>
        </Animated.View>
      )}

      {/* Error banner */}
      {error ? (
        <Animated.View
          entering={FadeInDown.duration(300)}
          style={[styles.errorBanner, {backgroundColor: theme.colors.error + '12', borderColor: theme.colors.error + '28'}]}>
          <Text style={{color: theme.colors.error, fontSize: 14, fontWeight: '500'}}>
            ⚠️ {error}
          </Text>
        </Animated.View>
      ) : null}

      {/* Loading skeleton */}
      {(loading && portfolios.length === 0) || importing
        ? renderLoadingSkeleton()
        : null}

      {/* Portfolio list */}
      <FlatList
        data={portfolios}
        renderItem={renderPortfolioCard}
        keyExtractor={item => String(item.id)}
        ListEmptyComponent={renderEmpty}
        refreshControl={
          <RefreshControl
            refreshing={loading && portfolios.length > 0}
            onRefresh={fetchPortfolios}
            tintColor={theme.colors.primary}
          />
        }
        contentContainerStyle={[styles.listContent, {paddingBottom: insets.bottom + 32}]}
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
}

function getGreeting(): string {
  const h = new Date().getHours();
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
}

const styles = StyleSheet.create({
  container: {flex: 1},

  // Header
  header: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    paddingHorizontal: 24,
    paddingBottom: 16,
  },
  headerLeft: {gap: 2},
  greeting: {fontSize: 13, fontWeight: '500', letterSpacing: 0.3},
  userName: {fontSize: 26, fontWeight: '800', letterSpacing: -0.8},
  avatarBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {fontSize: 18, fontWeight: '700'},

  // Quick actions
  quickActions: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 12,
    gap: 10,
    borderBottomWidth: 1,
  },
  quickAction: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
  },
  quickActionIcon: {fontSize: 14},
  quickActionText: {fontSize: 13, fontWeight: '600'},
  portfolioCount: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
    marginLeft: 'auto',
  },
  portfolioCountNum: {fontSize: 15, fontWeight: '800'},
  portfolioCountLabel: {fontSize: 12, fontWeight: '500'},

  // List
  listContent: {paddingHorizontal: 20, paddingTop: 16, flexGrow: 1},

  // Card
  portfolioCard: {marginBottom: 16, padding: 0},
  accentLine: {height: 3, borderTopLeftRadius: 18, borderTopRightRadius: 18},
  cardContent: {padding: 20},
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 14,
  },
  statusPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 20,
  },
  statusDot: {width: 6, height: 6, borderRadius: 3},
  statusText: {fontSize: 11, fontWeight: '700', letterSpacing: 0.5, textTransform: 'uppercase'},
  deleteBtn: {padding: 6},
  portfolioTitle: {fontSize: 20, fontWeight: '800', letterSpacing: -0.5, marginBottom: 6},
  portfolioTagline: {fontSize: 14, lineHeight: 21, marginBottom: 14},
  statsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 10,
    paddingTop: 16,
    borderTopWidth: 1,
  },
  stat: {flex: 1, alignItems: 'center', gap: 3},
  statDivider: {width: 1, height: 28},
  statValue: {fontSize: 20, fontWeight: '800'},
  statLabel: {fontSize: 10, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 0.6},

  // Empty state
  emptyState: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 40,
    paddingTop: 40,
    gap: 16,
  },
  emptyIcon: {
    width: 100,
    height: 100,
    borderRadius: 30,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  emptyTitle: {fontSize: 24, fontWeight: '800', letterSpacing: -0.5, textAlign: 'center'},
  emptySubtitle: {fontSize: 15, lineHeight: 23, textAlign: 'center'},
  emptyButton: {marginTop: 8},

  // Skeleton loading
  skeletonContainer: {paddingHorizontal: 20, paddingTop: 16, gap: 14},
  skeletonCard: {
    padding: 20,
    borderRadius: 18,
    borderWidth: 1,
  },
  skeletonStats: {flexDirection: 'row', justifyContent: 'space-around', marginTop: 20},

  // Importing banner
  importingBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 16,
    borderRadius: 14,
    borderWidth: 1,
  },
  importingText: {fontSize: 14, fontWeight: '600'},

  // Error
  errorBanner: {
    borderRadius: 12,
    borderWidth: 1,
    paddingHorizontal: 16,
    paddingVertical: 12,
    marginHorizontal: 20,
    marginTop: 8,
  },
});
