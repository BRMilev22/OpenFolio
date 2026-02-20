import React, {useEffect, useState} from 'react';
import {
  View,
  Text,
  StyleSheet,
  StatusBar,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  FlatList,
  Linking,
  Pressable,
} from 'react-native';
import Animated, {
  FadeInDown,
  FadeIn,
  FadeInRight,
  useSharedValue,
  useAnimatedStyle,
  withSpring,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import type {AppScreenProps} from '../../../navigation/types';
import {useTheme} from '../../../theme';
import {editorService} from '../services/editorService';
import {usePortfolioStore} from '../../dashboard/store/portfolioStore';
import {Card} from '../../../components/Card';
import {Skeleton} from '../../../components/AnimatedComponents';
import type {Project, Skill} from '../../../api/types/editor';
import type {Portfolio} from '../../../api/types/portfolio';

type Props = AppScreenProps<'Editor'>;
type Tab = 'projects' | 'skills';

const PROFICIENCY_COLOR: Record<string, string> = {
  BEGINNER: '#60A5FA',
  INTERMEDIATE: '#34D399',
  ADVANCED: '#A78BFA',
  EXPERT: '#F59E0B',
};

const PROFICIENCY_BG: Record<string, string> = {
  BEGINNER: '#60A5FA15',
  INTERMEDIATE: '#34D39915',
  ADVANCED: '#A78BFA15',
  EXPERT: '#F59E0B15',
};

export default function EditorScreen({route, navigation}: Props) {
  const {portfolioId} = route.params;
  const {theme} = useTheme();
  const insets = useSafeAreaInsets();

  const portfolio: Portfolio | undefined = usePortfolioStore(s =>
    s.portfolios.find(p => p.id === portfolioId),
  );

  const [tab, setTab] = useState<Tab>('projects');
  const [projects, setProjects] = useState<Project[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Animated tab indicator
  const tabIndicatorX = useSharedValue(0);
  const tabIndicatorStyle = useAnimatedStyle(() => ({
    transform: [{translateX: tabIndicatorX.value}],
  }));

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    Promise.all([
      editorService.getProjects(portfolioId),
      editorService.getSkills(portfolioId),
    ])
      .then(([p, s]) => {
        if (!active) return;
        setProjects(p);
        setSkills(s);
      })
      .catch(e => {
        if (!active) return;
        setError(e instanceof Error ? e.message : 'Failed to load');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [portfolioId]);

  const switchTab = (t: Tab) => {
    setTab(t);
    tabIndicatorX.value = withSpring(t === 'projects' ? 0 : 1, {damping: 18, stiffness: 180});
  };

  const renderProject = ({item, index}: {item: Project; index: number}) => (
    <Animated.View entering={FadeInDown.delay(index * 60 + 100).duration(450).springify().damping(16)}>
      <Card style={styles.projectCard} elevated>
        {/* Language accent strip */}
        <View style={[styles.projectAccent, {backgroundColor: getLanguageColor(item.languages[0])}]} />
        <View style={styles.projectInner}>
          <View style={styles.projectHeader}>
            <View style={styles.projectTitleRow}>
              {item.highlighted ? (
                <View style={[styles.starBadge, {backgroundColor: '#F59E0B18'}]}>
                  <Text style={{fontSize: 10}}>⭐</Text>
                </View>
              ) : null}
              <Text
                style={[styles.projectName, {color: theme.colors.text}]}
                numberOfLines={1}>
                {item.name}
              </Text>
            </View>
            {item.url ? (
              <TouchableOpacity
                onPress={() => item.url && Linking.openURL(item.url)}
                activeOpacity={0.7}
                style={[styles.linkBtn, {backgroundColor: theme.colors.primary + '12'}]}>
                <Text style={[styles.linkIcon, {color: theme.colors.primary}]}>↗</Text>
              </TouchableOpacity>
            ) : null}
          </View>

          {item.description ? (
            <Text
              style={[styles.projectDesc, {color: theme.colors.textSecondary}]}
              numberOfLines={3}>
              {item.description}
            </Text>
          ) : null}

          <View style={styles.projectFooter}>
            <View style={styles.langPills}>
              {item.languages.slice(0, 3).map(lang => (
                <View
                  key={lang}
                  style={[
                    styles.langPill,
                    {backgroundColor: getLanguageColor(lang) + '18', borderColor: getLanguageColor(lang) + '33'},
                  ]}>
                  <View style={[styles.langDot, {backgroundColor: getLanguageColor(lang)}]} />
                  <Text style={[styles.langText, {color: theme.colors.text}]}>{lang}</Text>
                </View>
              ))}
            </View>
            <View style={styles.metaRow}>
              {item.stars > 0 ? (
                <View style={styles.metaItem}>
                  <Text style={[styles.metaIcon, {color: '#F59E0B'}]}>★</Text>
                  <Text style={[styles.metaValue, {color: theme.colors.textSecondary}]}>{item.stars}</Text>
                </View>
              ) : null}
              {item.forks > 0 ? (
                <View style={styles.metaItem}>
                  <Text style={[styles.metaIcon, {color: theme.colors.textMuted ?? '#52525B'}]}>⑂</Text>
                  <Text style={[styles.metaValue, {color: theme.colors.textSecondary}]}>{item.forks}</Text>
                </View>
              ) : null}
            </View>
          </View>
        </View>
      </Card>
    </Animated.View>
  );

  // Group skills by category
  const skillsByCategory = skills.reduce<Record<string, Skill[]>>((acc, skill) => {
    const cat = skill.category ?? 'Other';
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(skill);
    return acc;
  }, {});

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <StatusBar barStyle="light-content" backgroundColor={theme.colors.background} />

      {/* Header */}
      <Animated.View
        entering={FadeIn.duration(300)}
        style={[styles.header, {paddingTop: insets.top + 12}]}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          style={styles.backBtn}
          activeOpacity={0.7}>
          <Text style={[styles.backText, {color: theme.colors.primary}]}>←</Text>
        </TouchableOpacity>
        <View style={styles.headerCenter}>
          <Text
            style={[styles.headerTitle, {color: theme.colors.text}]}
            numberOfLines={1}>
            {portfolio?.title ?? 'Portfolio'}
          </Text>
          <View style={styles.headerMeta}>
            <View
              style={[
                styles.statusDot,
                {
                  backgroundColor: portfolio?.published
                    ? theme.colors.success
                    : theme.colors.textMuted ?? '#52525B',
                },
              ]}
            />
            <Text
              style={[
                styles.headerSubtitle,
                {
                  color: portfolio?.published
                    ? theme.colors.success
                    : theme.colors.textMuted ?? '#52525B',
                },
              ]}>
              {portfolio?.published ? 'Published' : 'Draft'}
            </Text>
          </View>
        </View>
        <View style={styles.headerRight} />
      </Animated.View>

      {/* Animated Stats Cards */}
      {portfolio ? (
        <Animated.View
          entering={FadeInDown.delay(100).duration(400).springify()}
          style={styles.statsCards}>
          {[
            {value: portfolio.projectCount, label: 'Repos', icon: '📦', color: theme.colors.primary},
            {value: portfolio.skillCount, label: 'Skills', icon: '🛠', color: theme.colors.secondary ?? theme.colors.primary},
            {value: portfolio.published ? 1 : 0, label: portfolio.published ? 'Live' : 'Draft', icon: portfolio.published ? '🌐' : '📝', color: portfolio.published ? theme.colors.success : theme.colors.textMuted ?? '#52525B'},
          ].map((s, i) => (
            <View
              key={i}
              style={[styles.statCard, {
                backgroundColor: theme.colors.card ?? theme.colors.surface,
                borderColor: theme.colors.cardBorder ?? theme.colors.border,
              }]}>
              <Text style={styles.statCardIcon}>{s.icon}</Text>
              <Text style={[styles.statCardValue, {color: s.color}]}>{s.value}</Text>
              <Text style={[styles.statCardLabel, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>{s.label}</Text>
            </View>
          ))}
        </Animated.View>
      ) : null}

      {/* Tabs — animated indicator */}
      <Animated.View
        entering={FadeInDown.delay(150).duration(400).springify()}
        style={[styles.tabContainer, {borderBottomColor: theme.colors.borderLight ?? theme.colors.border}]}>
        <View style={styles.tabsWrapper}>
          {(['projects', 'skills'] as Tab[]).map(t => (
            <Pressable
              key={t}
              onPress={() => switchTab(t)}
              style={[styles.tab]}>
              <Text
                style={[
                  styles.tabText,
                  {
                    color: tab === t ? theme.colors.primary : theme.colors.textMuted ?? theme.colors.textSecondary,
                    fontWeight: tab === t ? '700' : '500',
                  },
                ]}>
                {t === 'projects'
                  ? `Projects${projects.length > 0 ? ` (${projects.length})` : ''}`
                  : `Skills${skills.length > 0 ? ` (${skills.length})` : ''}`}
              </Text>
              {tab === t && (
                <Animated.View
                  entering={FadeIn.duration(200)}
                  style={[styles.tabActiveIndicator, {backgroundColor: theme.colors.primary}]}
                />
              )}
            </Pressable>
          ))}
        </View>
      </Animated.View>

      {/* Content */}
      {loading ? (
        <View style={styles.loadingContainer}>
          {[0, 1, 2, 3].map(i => (
            <Animated.View
              key={i}
              entering={FadeIn.delay(i * 100).duration(400)}
              style={[styles.skeletonCard, {backgroundColor: theme.colors.card ?? theme.colors.surface, borderColor: theme.colors.cardBorder ?? theme.colors.border}]}>
              <Skeleton width="40%" height={14} color={theme.colors.border} />
              <Skeleton width="90%" height={10} color={theme.colors.border} style={{marginTop: 10}} />
              <Skeleton width="60%" height={10} color={theme.colors.border} style={{marginTop: 6}} />
              <View style={{flexDirection: 'row', gap: 8, marginTop: 14}}>
                <Skeleton width={60} height={22} borderRadius={6} color={theme.colors.border} />
                <Skeleton width={50} height={22} borderRadius={6} color={theme.colors.border} />
              </View>
            </Animated.View>
          ))}
        </View>
      ) : error ? (
        <View style={styles.centered}>
          <Text style={[styles.errorText, {color: theme.colors.error}]}>⚠️ {error}</Text>
        </View>
      ) : tab === 'projects' ? (
        <FlatList
          data={projects}
          renderItem={renderProject}
          keyExtractor={item => String(item.id)}
          contentContainerStyle={[styles.listContent, {paddingBottom: insets.bottom + 100}]}
          showsVerticalScrollIndicator={false}
          ListEmptyComponent={
            <View style={styles.centered}>
              <Text style={{fontSize: 32, marginBottom: 8}}>📦</Text>
              <Text style={[styles.emptyText, {color: theme.colors.textSecondary}]}>
                No projects imported yet
              </Text>
            </View>
          }
        />
      ) : (
        <ScrollView
          contentContainerStyle={[styles.listContent, {paddingBottom: insets.bottom + 100}]}
          showsVerticalScrollIndicator={false}>
          {Object.entries(skillsByCategory).map(([category, categorySkills], catIndex) => (
            <Animated.View
              key={category}
              entering={FadeInDown.delay(catIndex * 80 + 100).duration(450).springify().damping(16)}
              style={styles.skillGroup}>
              <View style={styles.categoryHeader}>
                <View style={[styles.categoryDot, {backgroundColor: theme.colors.primary}]} />
                <Text style={[styles.categoryTitle, {color: theme.colors.textSecondary}]}>
                  {category.toUpperCase()}
                </Text>
                <Text style={[styles.categoryCount, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
                  {categorySkills.length}
                </Text>
              </View>
              <View style={styles.skillPills}>
                {categorySkills.map((skill, skillIndex) => (
                  <Animated.View
                    key={skill.id}
                    entering={FadeIn.delay(catIndex * 60 + skillIndex * 30 + 200).duration(300)}>
                    <View
                      style={[
                        styles.skillPill,
                        {
                          backgroundColor: PROFICIENCY_BG[skill.proficiency] ?? theme.colors.primary + '12',
                          borderColor: (PROFICIENCY_COLOR[skill.proficiency] ?? theme.colors.primary) + '33',
                        },
                      ]}>
                      <Text style={[styles.skillName, {color: theme.colors.text}]}>
                        {skill.name}
                      </Text>
                      <View
                        style={[
                          styles.skillLevelBadge,
                          {backgroundColor: (PROFICIENCY_COLOR[skill.proficiency] ?? theme.colors.primary) + '22'},
                        ]}>
                        <Text
                          style={[
                            styles.skillLevel,
                            {color: PROFICIENCY_COLOR[skill.proficiency] ?? theme.colors.primary},
                          ]}>
                          {skill.proficiency.charAt(0) + skill.proficiency.slice(1).toLowerCase()}
                        </Text>
                      </View>
                    </View>
                  </Animated.View>
                ))}
              </View>
            </Animated.View>
          ))}
          {skills.length === 0 ? (
            <View style={styles.centered}>
              <Text style={{fontSize: 32, marginBottom: 8}}>🛠</Text>
              <Text style={[styles.emptyText, {color: theme.colors.textSecondary}]}>
                No skills detected yet
              </Text>
            </View>
          ) : null}
        </ScrollView>
      )}

      {/* Floating Action Footer */}
      <Animated.View
        entering={FadeInDown.delay(300).duration(400).springify()}
        style={[
          styles.footer,
          {
            paddingBottom: insets.bottom + 12,
            backgroundColor: theme.colors.background + 'F5',
            borderTopColor: theme.colors.borderLight ?? theme.colors.border,
          },
        ]}>
        {[
          {icon: '👁', label: 'Preview', key: 'Preview', primary: false},
          {icon: '📝', label: 'Resume', key: 'ResumeBuilder', primary: true},
          {icon: '🌐', label: 'Publish', key: 'Publish', primary: false},
          {icon: '📄', label: 'PDF', key: 'Export', primary: false},
        ].map(btn => (
          <TouchableOpacity
            key={btn.key}
            onPress={() => {
              if (btn.key === 'ResumeBuilder') {
                navigation.navigate('ResumeBuilder', {portfolioId});
              } else if (btn.key === 'Preview') {
                navigation.navigate('Preview', {portfolioId});
              } else if (btn.key === 'Publish') {
                navigation.navigate('Publish', {portfolioId});
              } else {
                navigation.navigate('Export', {portfolioId});
              }
            }}
            activeOpacity={0.8}
            style={[
              styles.footerBtn,
              btn.primary
                ? {
                    backgroundColor: theme.colors.primary,
                    borderColor: theme.colors.primary,
                    shadowColor: theme.colors.shadowColor ?? theme.colors.primary,
                    shadowOffset: {width: 0, height: 4},
                    shadowOpacity: 0.3,
                    shadowRadius: 8,
                    elevation: 4,
                  }
                : {
                    backgroundColor: theme.colors.card ?? theme.colors.surface,
                    borderColor: theme.colors.cardBorder ?? theme.colors.border,
                  },
            ]}>
            <Text style={styles.footerBtnIcon}>{btn.icon}</Text>
            <Text
              style={[
                styles.footerBtnText,
                {color: btn.primary ? '#fff' : theme.colors.text},
              ]}>
              {btn.label}
            </Text>
          </TouchableOpacity>
        ))}
      </Animated.View>
    </View>
  );
}

// Map language to a nice color
function getLanguageColor(lang?: string): string {
  if (!lang) return '#8B5CF6';
  const map: Record<string, string> = {
    JavaScript: '#F7DF1E',
    TypeScript: '#3178C6',
    Python: '#3776AB',
    Java: '#ED8B00',
    'C++': '#00599C',
    C: '#A8B9CC',
    'C#': '#239120',
    Go: '#00ADD8',
    Rust: '#DEA584',
    Ruby: '#CC342D',
    PHP: '#777BB4',
    Swift: '#FA7343',
    Kotlin: '#7F52FF',
    Dart: '#0175C2',
    HTML: '#E34F26',
    CSS: '#1572B6',
    Shell: '#89E051',
    Scala: '#DC322F',
    R: '#276DC3',
  };
  return map[lang] ?? '#8B5CF6';
}

const styles = StyleSheet.create({
  container: {flex: 1},

  // Header
  header: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: 20,
    paddingBottom: 12,
    gap: 12,
  },
  backBtn: {paddingRight: 4, paddingBottom: 2},
  backText: {fontSize: 26, fontWeight: '300', lineHeight: 30},
  headerCenter: {flex: 1, gap: 3},
  headerTitle: {fontSize: 20, fontWeight: '800', letterSpacing: -0.5},
  headerMeta: {flexDirection: 'row', alignItems: 'center', gap: 6},
  statusDot: {width: 6, height: 6, borderRadius: 3},
  headerSubtitle: {fontSize: 11, fontWeight: '600', letterSpacing: 0.3},
  headerRight: {width: 32},

  // Stats cards
  statsCards: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    gap: 8,
    paddingVertical: 10,
  },
  statCard: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 12,
    borderRadius: 14,
    borderWidth: 1,
    gap: 2,
  },
  statCardIcon: {fontSize: 16, marginBottom: 2},
  statCardValue: {fontSize: 20, fontWeight: '800'},
  statCardLabel: {fontSize: 10, fontWeight: '600', letterSpacing: 0.5, textTransform: 'uppercase'},

  // Tabs
  tabContainer: {borderBottomWidth: 1, paddingHorizontal: 16},
  tabsWrapper: {flexDirection: 'row'},
  tab: {
    flex: 1,
    paddingVertical: 14,
    alignItems: 'center',
    position: 'relative',
  },
  tabText: {fontSize: 14, letterSpacing: 0.2},
  tabActiveIndicator: {
    position: 'absolute',
    bottom: 0,
    left: '20%',
    right: '20%',
    height: 2.5,
    borderRadius: 2,
  },

  // Content
  listContent: {paddingHorizontal: 16, paddingTop: 14, flexGrow: 1},

  // Project card
  projectCard: {marginBottom: 12, padding: 0},
  projectAccent: {height: 3, borderTopLeftRadius: 18, borderTopRightRadius: 18},
  projectInner: {padding: 16},
  projectHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  projectTitleRow: {flexDirection: 'row', alignItems: 'center', flex: 1, marginRight: 8, gap: 6},
  starBadge: {
    width: 22,
    height: 22,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  projectName: {fontSize: 16, fontWeight: '700', letterSpacing: -0.3, flex: 1},
  linkBtn: {
    width: 32,
    height: 32,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  linkIcon: {fontSize: 16, fontWeight: '600'},
  projectDesc: {fontSize: 13, lineHeight: 20, marginBottom: 12},
  projectFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 4,
  },
  langPills: {flexDirection: 'row', flexWrap: 'wrap', gap: 6},
  langPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
    borderWidth: 1,
  },
  langDot: {width: 7, height: 7, borderRadius: 4},
  langText: {fontSize: 11, fontWeight: '600'},
  metaRow: {flexDirection: 'row', alignItems: 'center', gap: 10},
  metaItem: {flexDirection: 'row', alignItems: 'center', gap: 3},
  metaIcon: {fontSize: 12},
  metaValue: {fontSize: 12, fontWeight: '600'},

  // Skill groups
  skillGroup: {marginBottom: 22},
  categoryHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 10,
  },
  categoryDot: {width: 4, height: 14, borderRadius: 2},
  categoryTitle: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1,
    flex: 1,
  },
  categoryCount: {
    fontSize: 11,
    fontWeight: '600',
  },
  skillPills: {flexDirection: 'row', flexWrap: 'wrap', gap: 8},
  skillPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingLeft: 12,
    paddingRight: 6,
    paddingVertical: 7,
    borderRadius: 10,
    borderWidth: 1,
  },
  skillName: {fontSize: 13, fontWeight: '600'},
  skillLevelBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  skillLevel: {fontSize: 10, fontWeight: '600'},

  // Loading
  loadingContainer: {paddingHorizontal: 16, paddingTop: 14, gap: 12},
  skeletonCard: {padding: 16, borderRadius: 16, borderWidth: 1},

  // Empty / error
  centered: {flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 60},
  emptyText: {fontSize: 15, fontWeight: '500'},
  errorText: {fontSize: 15, fontWeight: '600'},

  // Footer
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
    gap: 2,
  },
  footerBtnIcon: {fontSize: 16},
  footerBtnText: {fontSize: 11, fontWeight: '700'},
});
