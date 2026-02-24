import React, {useEffect, useState, useCallback} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  TextInput,
  ActivityIndicator,
  Alert,
  StatusBar,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import Animated, {FadeInDown, FadeIn, FadeInUp} from 'react-native-reanimated';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import type {AppScreenProps} from '../../../navigation/types';
import {useTheme} from '../../../theme';
import {resumeService} from '../services/resumeService';
import {Skeleton, PulseView} from '../../../components/AnimatedComponents';
import {Button} from '../../../components/Button';
import type {ResumeInfo} from '../../../api/types/resume';

type Props = AppScreenProps<'ResumeBuilder'>;

interface FieldConfig {
  key: string;
  label: string;
  icon: string;
  placeholder: string;
  keyboard?: 'default' | 'email-address' | 'phone-pad' | 'url';
  autoCapitalize?: 'none' | 'sentences' | 'words';
  multiline?: boolean;
  height?: number;
}

const CONTACT_FIELDS: FieldConfig[] = [
  {key: 'fullName', label: 'Full Name', icon: '👤', placeholder: 'John Doe', autoCapitalize: 'words'},
  {key: 'jobTitle', label: 'Job Title', icon: '💼', placeholder: 'Full-Stack Software Engineer', autoCapitalize: 'words'},
  {key: 'email', label: 'Email', icon: '📧', placeholder: 'john@example.com', keyboard: 'email-address', autoCapitalize: 'none'},
  {key: 'phone', label: 'Phone', icon: '📱', placeholder: '+1 (555) 123-4567', keyboard: 'phone-pad'},
  {key: 'location', label: 'Location', icon: '📍', placeholder: 'San Francisco, CA', autoCapitalize: 'words'},
];

const LINK_FIELDS: FieldConfig[] = [
  {key: 'website', label: 'Website', icon: '🌐', placeholder: 'https://johndoe.dev', autoCapitalize: 'none'},
  {key: 'githubUrl', label: 'GitHub', icon: '🐙', placeholder: 'https://github.com/johndoe', autoCapitalize: 'none'},
  {key: 'linkedinUrl', label: 'LinkedIn', icon: '💼', placeholder: 'https://linkedin.com/in/johndoe', autoCapitalize: 'none'},
];

export default function ResumeBuilderScreen({route, navigation}: Props) {
  const {portfolioId} = route.params;
  const {theme} = useTheme();
  const insets = useSafeAreaInsets();

  const [resumes, setResumes] = useState<ResumeInfo[]>([]);
  const [activeResume, setActiveResume] = useState<ResumeInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [saving, setSaving] = useState(false);

  // Editable fields
  const [fields, setFields] = useState<Record<string, string>>({
    fullName: '', jobTitle: '', email: '', phone: '', location: '',
    website: '', githubUrl: '', linkedinUrl: '', summary: '',
  });

  const [expandedSection, setExpandedSection] = useState<string | null>('contact');

  const updateField = (key: string, value: string) => {
    setFields(prev => ({...prev, [key]: value}));
  };

  const loadResumes = useCallback(async () => {
    setLoading(true);
    try {
      const all = await resumeService.list();
      const filtered = all.filter(r => r.portfolioId === portfolioId);
      setResumes(filtered);
      if (filtered.length > 0 && !activeResume) {
        selectResume(filtered[0]);
      }
    } catch (e: any) {
      Alert.alert('Error', e?.message ?? 'Failed to load resumes');
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [portfolioId]);

  useEffect(() => {
    loadResumes();
  }, [loadResumes]);

  const selectResume = (r: ResumeInfo) => {
    setActiveResume(r);
    setFields({
      fullName: r.fullName ?? '',
      jobTitle: r.jobTitle ?? '',
      email: r.email ?? '',
      phone: r.phone ?? '',
      location: r.location ?? '',
      website: r.website ?? '',
      githubUrl: r.githubUrl ?? '',
      linkedinUrl: r.linkedinUrl ?? '',
      summary: r.summary ?? '',
    });
  };

  const handleCreate = async () => {
    setCreating(true);
    try {
      const newResume = await resumeService.create({portfolioId, title: 'My Resume'});
      setResumes(prev => [newResume, ...prev]);
      selectResume(newResume);
    } catch (e: any) {
      Alert.alert('Error', e?.message ?? 'Failed to create resume');
    } finally {
      setCreating(false);
    }
  };

  const handleSave = async () => {
    if (!activeResume) return;
    setSaving(true);
    try {
      const updated = await resumeService.update(activeResume.id, {
        fullName: fields.fullName,
        jobTitle: fields.jobTitle,
        email: fields.email,
        phone: fields.phone,
        location: fields.location,
        website: fields.website,
        linkedinUrl: fields.linkedinUrl,
        githubUrl: fields.githubUrl,
        summary: fields.summary,
      });
      setActiveResume(updated);
      setResumes(prev => prev.map(r => (r.id === updated.id ? updated : r)));
      Alert.alert('✅ Saved', 'Your resume has been updated.');
    } catch (e: any) {
      Alert.alert('Error', e?.message ?? 'Failed to save resume');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = () => {
    if (!activeResume) return;
    Alert.alert('Delete Resume', 'Are you sure? This cannot be undone.', [
      {text: 'Cancel', style: 'cancel'},
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          try {
            await resumeService.delete(activeResume.id);
            setResumes(prev => prev.filter(r => r.id !== activeResume.id));
            setActiveResume(null);
          } catch (e: any) {
            Alert.alert('Error', e?.message ?? 'Failed to delete');
          }
        },
      },
    ]);
  };

  const toggleSection = (section: string) => {
    setExpandedSection(prev => (prev === section ? null : section));
  };

  if (loading) {
    return (
      <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
        <StatusBar barStyle="light-content" />
        <View style={[styles.header, {paddingTop: insets.top + 12, borderBottomColor: theme.colors.border}]}>
          <TouchableOpacity onPress={() => navigation.goBack()} activeOpacity={0.7}>
            <Text style={[styles.backText, {color: theme.colors.primary}]}>←</Text>
          </TouchableOpacity>
          <Text style={[styles.headerTitle, {color: theme.colors.text}]}>Resume Builder</Text>
          <View style={{width: 32}} />
        </View>
        <View style={styles.loadingContainer}>
          {[0, 1, 2].map(i => (
            <Animated.View key={i} entering={FadeIn.delay(i * 100).duration(400)} style={[styles.skeletonBlock, {backgroundColor: theme.colors.card ?? theme.colors.surface, borderColor: theme.colors.cardBorder ?? theme.colors.border}]}>
              <Skeleton width="40%" height={12} color={theme.colors.border} />
              <Skeleton width="100%" height={48} borderRadius={12} color={theme.colors.border} style={{marginTop: 10}} />
              <Skeleton width="100%" height={48} borderRadius={12} color={theme.colors.border} style={{marginTop: 8}} />
            </Animated.View>
          ))}
        </View>
      </View>
    );
  }

  // No resume yet
  if (!activeResume) {
    return (
      <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
        <StatusBar barStyle="light-content" />
        <View style={[styles.header, {paddingTop: insets.top + 12, borderBottomColor: theme.colors.border}]}>
          <TouchableOpacity onPress={() => navigation.goBack()} activeOpacity={0.7}>
            <Text style={[styles.backText, {color: theme.colors.primary}]}>←</Text>
          </TouchableOpacity>
          <Text style={[styles.headerTitle, {color: theme.colors.text}]}>Resume Builder</Text>
          <View style={{width: 32}} />
        </View>
        <Animated.View entering={FadeInDown.delay(200).duration(500).springify()} style={[styles.centered, {flex: 1}]}>
          <PulseView>
            <View style={[styles.emptyIcon, {backgroundColor: theme.colors.primary + '12', borderColor: theme.colors.primary + '25', shadowColor: theme.colors.primary, shadowOffset: {width: 0, height: 0}, shadowOpacity: 0.2, shadowRadius: 20}]}>
              <Text style={{fontSize: 44}}>📝</Text>
            </View>
          </PulseView>
          <Text style={[styles.emptyTitle, {color: theme.colors.text}]}>Create Your Resume</Text>
          <Text style={[styles.emptySubtitle, {color: theme.colors.textSecondary}]}>
            Generate a professional resume from your GitHub portfolio data. AI will craft bullet points automatically.
          </Text>
          <Button
            label="✨ Generate Resume"
            onPress={handleCreate}
            loading={creating}
            disabled={creating}
            size="lg"
            fullWidth={false}
          />
        </Animated.View>
      </View>
    );
  }

  const renderSection = (
    sectionKey: string,
    title: string,
    icon: string,
    fieldList: FieldConfig[],
    index: number,
  ) => {
    const isExpanded = expandedSection === sectionKey;
    return (
      <Animated.View
        key={sectionKey}
        entering={FadeInDown.delay(index * 80 + 100).duration(450).springify().damping(16)}>
        <TouchableOpacity
          onPress={() => toggleSection(sectionKey)}
          activeOpacity={0.7}
          style={[styles.sectionHeader, {
            backgroundColor: theme.colors.card ?? theme.colors.surface,
            borderColor: isExpanded ? theme.colors.primary + '44' : theme.colors.cardBorder ?? theme.colors.border,
          }]}>
          <View style={[styles.sectionIconWrap, {backgroundColor: theme.colors.primary + '15'}]}>
            <Text style={styles.sectionIcon}>{icon}</Text>
          </View>
          <View style={{flex: 1}}>
            <Text style={[styles.sectionTitle, {color: theme.colors.text}]}>{title}</Text>
            <Text style={[styles.sectionHint, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
              {fieldList.filter(f => fields[f.key]?.trim()).length}/{fieldList.length} filled
            </Text>
          </View>
          <Text style={[styles.chevron, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
            {isExpanded ? '▲' : '▼'}
          </Text>
        </TouchableOpacity>

        {isExpanded && (
          <Animated.View
            entering={FadeInDown.duration(300).springify()}
            style={[styles.sectionContent, {
              backgroundColor: theme.colors.card ?? theme.colors.surface,
              borderColor: theme.colors.primary + '22',
            }]}>
            {fieldList.map((field, fi) => (
              <View key={field.key} style={[styles.fieldGroup, fi > 0 && styles.fieldGroupSpaced]}>
                <View style={styles.fieldLabelRow}>
                  <Text style={styles.fieldIcon}>{field.icon}</Text>
                  <Text style={[styles.fieldLabel, {color: theme.colors.textSecondary}]}>{field.label}</Text>
                </View>
                <TextInput
                  value={fields[field.key]}
                  onChangeText={(v) => updateField(field.key, v)}
                  placeholder={field.placeholder}
                  placeholderTextColor={theme.colors.textMuted ?? '#52525B'}
                  keyboardType={field.keyboard ?? 'default'}
                  autoCapitalize={field.autoCapitalize ?? 'sentences'}
                  multiline={field.multiline}
                  numberOfLines={field.multiline ? 5 : 1}
                  textAlignVertical={field.multiline ? 'top' : 'center'}
                  style={[
                    styles.fieldInput,
                    {
                      color: theme.colors.text,
                      backgroundColor: theme.colors.surfaceElevated ?? theme.colors.surface,
                      borderColor: theme.colors.borderLight ?? theme.colors.border,
                      ...(field.multiline && {height: field.height ?? 120, paddingTop: 14}),
                    },
                  ]}
                />
              </View>
            ))}
          </Animated.View>
        )}
      </Animated.View>
    );
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
          Resume Builder
        </Text>
        <TouchableOpacity onPress={handleDelete} activeOpacity={0.7} style={styles.deleteBtn}>
          <Text style={{color: theme.colors.error + '88', fontSize: 14}}>🗑</Text>
        </TouchableOpacity>
      </Animated.View>

      {/* Multiple resumes tabs */}
      {resumes.length > 1 && (
        <Animated.View entering={FadeIn.delay(50).duration(300)}>
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            style={[styles.tabBar, {borderBottomColor: theme.colors.borderLight ?? theme.colors.border}]}
            contentContainerStyle={{paddingHorizontal: 16, gap: 8}}>
            {resumes.map(r => (
              <TouchableOpacity
                key={r.id}
                onPress={() => selectResume(r)}
                activeOpacity={0.7}
                style={[
                  styles.tabChip,
                  {
                    backgroundColor: r.id === activeResume.id ? theme.colors.primary + '18' : 'transparent',
                    borderColor: r.id === activeResume.id ? theme.colors.primary : theme.colors.border,
                  },
                ]}>
                <Text
                  style={[
                    styles.tabChipText,
                    {color: r.id === activeResume.id ? theme.colors.primary : theme.colors.textSecondary},
                  ]}
                  numberOfLines={1}>
                  {r.title}
                </Text>
              </TouchableOpacity>
            ))}
            <TouchableOpacity
              onPress={handleCreate}
              activeOpacity={0.7}
              style={[styles.tabChip, {borderColor: theme.colors.border, borderStyle: 'dashed'}]}>
              <Text style={[styles.tabChipText, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>+ New</Text>
            </TouchableOpacity>
          </ScrollView>
        </Animated.View>
      )}

      <KeyboardAvoidingView
        style={{flex: 1}}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={0}>
        <ScrollView
          contentContainerStyle={[styles.formContent, {paddingBottom: insets.bottom + 120}]}
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled">

          {/* Template badge */}
          <Animated.View entering={FadeInDown.delay(50).duration(400).springify()}>
            <TouchableOpacity
              onPress={() => navigation.navigate('ResumeTemplates', {resumeId: activeResume.id})}
              activeOpacity={0.7}
              style={[styles.templateBadge, {backgroundColor: theme.colors.card ?? theme.colors.surface, borderColor: theme.colors.primary + '33'}]}>
              <View style={[styles.templateIconWrap, {backgroundColor: theme.colors.primary + '15'}]}>
                <Text style={{fontSize: 22}}>🎨</Text>
              </View>
              <View style={{flex: 1}}>
                <Text style={[styles.templateLabel, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>TEMPLATE</Text>
                <Text style={[styles.templateName, {color: theme.colors.text}]}>
                  {activeResume.templateKey.charAt(0).toUpperCase() + activeResume.templateKey.slice(1)}
                </Text>
              </View>
              <View style={[styles.changeBadge, {backgroundColor: theme.colors.primary + '18'}]}>
                <Text style={{color: theme.colors.primary, fontSize: 12, fontWeight: '700'}}>Change →</Text>
              </View>
            </TouchableOpacity>
          </Animated.View>

          {/* Collapsible sections */}
          {renderSection('contact', 'Contact Information', '👤', CONTACT_FIELDS, 0)}
          {renderSection('links', 'Links & Social', '🔗', LINK_FIELDS, 1)}

          {/* Summary section — special treatment */}
          <Animated.View entering={FadeInDown.delay(260).duration(450).springify().damping(16)}>
            <TouchableOpacity
              onPress={() => toggleSection('summary')}
              activeOpacity={0.7}
              style={[styles.sectionHeader, {
                backgroundColor: theme.colors.card ?? theme.colors.surface,
                borderColor: expandedSection === 'summary' ? theme.colors.primary + '44' : theme.colors.cardBorder ?? theme.colors.border,
              }]}>
              <View style={[styles.sectionIconWrap, {backgroundColor: theme.colors.primary + '15'}]}>
                <Text style={styles.sectionIcon}>✍️</Text>
              </View>
              <View style={{flex: 1}}>
                <Text style={[styles.sectionTitle, {color: theme.colors.text}]}>Professional Summary</Text>
                <Text style={[styles.sectionHint, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
                  {fields.summary?.trim() ? `${fields.summary.trim().split(' ').length} words` : 'Not written yet'}
                </Text>
              </View>
              <Text style={[styles.chevron, {color: theme.colors.textMuted ?? theme.colors.textSecondary}]}>
                {expandedSection === 'summary' ? '▲' : '▼'}
              </Text>
            </TouchableOpacity>

            {expandedSection === 'summary' && (
              <Animated.View
                entering={FadeInDown.duration(300).springify()}
                style={[styles.sectionContent, {
                  backgroundColor: theme.colors.card ?? theme.colors.surface,
                  borderColor: theme.colors.primary + '22',
                }]}>
                <TextInput
                  value={fields.summary}
                  onChangeText={(v) => updateField('summary', v)}
                  placeholder="Experienced software engineer with expertise in building scalable web applications..."
                  placeholderTextColor={theme.colors.textMuted ?? '#52525B'}
                  multiline
                  numberOfLines={6}
                  textAlignVertical="top"
                  style={[
                    styles.fieldInput,
                    {
                      color: theme.colors.text,
                      backgroundColor: theme.colors.surfaceElevated ?? theme.colors.surface,
                      borderColor: theme.colors.borderLight ?? theme.colors.border,
                      height: 140,
                      paddingTop: 14,
                    },
                  ]}
                />
                <Text style={[styles.summaryTip, {color: theme.colors.info ?? theme.colors.primary}]}>
                  💡 Tip: Write 60-100 words in third person. AI will enhance this when generating your PDF.
                </Text>
              </Animated.View>
            )}
          </Animated.View>

          {/* Auto-included sections info */}
          <Animated.View entering={FadeInDown.delay(340).duration(450).springify()}>
            <View style={[styles.infoCard, {backgroundColor: theme.colors.primary + '08', borderColor: theme.colors.primary + '18'}]}>
              <Text style={[styles.infoTitle, {color: theme.colors.primary}]}>📋 Auto-included sections</Text>
              <Text style={[styles.infoText, {color: theme.colors.textSecondary}]}>
                Projects, skills, experience, and education are automatically pulled from your portfolio. Choose a template and preview to see your complete resume.
              </Text>
            </View>
          </Animated.View>
        </ScrollView>
      </KeyboardAvoidingView>

      {/* Footer */}
      <Animated.View
        entering={FadeInUp.delay(400).duration(400).springify()}
        style={[styles.footer, {
          paddingBottom: insets.bottom + 12,
          backgroundColor: theme.colors.background + 'F5',
          borderTopColor: theme.colors.borderLight ?? theme.colors.border,
        }]}>
        <TouchableOpacity
          onPress={handleSave}
          disabled={saving}
          activeOpacity={0.8}
          style={[styles.footerBtn, {
            backgroundColor: theme.colors.card ?? theme.colors.surface,
            borderColor: theme.colors.cardBorder ?? theme.colors.border,
            opacity: saving ? 0.6 : 1,
          }]}>
          {saving ? (
            <ActivityIndicator size="small" color={theme.colors.primary} />
          ) : (
            <>
              <Text style={styles.footerBtnIcon}>💾</Text>
              <Text style={[styles.footerBtnText, {color: theme.colors.text}]}>Save</Text>
            </>
          )}
        </TouchableOpacity>
        <TouchableOpacity
          onPress={() => navigation.navigate('ResumePreview', {resumeId: activeResume.id})}
          activeOpacity={0.8}
          style={[styles.footerBtn, {
            backgroundColor: theme.colors.primary,
            borderColor: theme.colors.primary,
            shadowColor: theme.colors.shadowColor ?? theme.colors.primary,
            shadowOffset: {width: 0, height: 4},
            shadowOpacity: 0.3,
            shadowRadius: 8,
            elevation: 4,
          }]}>
          <Text style={styles.footerBtnIcon}>👁</Text>
          <Text style={[styles.footerBtnText, {color: '#fff'}]}>Preview</Text>
        </TouchableOpacity>
        <TouchableOpacity
          onPress={() => navigation.navigate('ResumeTemplates', {resumeId: activeResume.id})}
          activeOpacity={0.8}
          style={[styles.footerBtn, {
            backgroundColor: theme.colors.card ?? theme.colors.surface,
            borderColor: theme.colors.cardBorder ?? theme.colors.border,
          }]}>
          <Text style={styles.footerBtnIcon}>🎨</Text>
          <Text style={[styles.footerBtnText, {color: theme.colors.text}]}>Style</Text>
        </TouchableOpacity>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1},
  centered: {alignItems: 'center', justifyContent: 'center', gap: 16, paddingHorizontal: 32},

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
  headerTitle: {flex: 1, fontSize: 20, fontWeight: '800', letterSpacing: -0.5},
  deleteBtn: {padding: 6},

  // Tabs
  tabBar: {maxHeight: 56, borderBottomWidth: 1, paddingVertical: 10},
  tabChip: {paddingHorizontal: 16, paddingVertical: 7, borderRadius: 20, borderWidth: 1},
  tabChipText: {fontSize: 13, fontWeight: '600'},

  // Form
  formContent: {paddingHorizontal: 20, paddingTop: 16, gap: 12},

  // Template badge
  templateBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 14,
    borderRadius: 16,
    borderWidth: 1.5,
  },
  templateIconWrap: {width: 44, height: 44, borderRadius: 12, alignItems: 'center', justifyContent: 'center'},
  templateLabel: {fontSize: 10, fontWeight: '700', letterSpacing: 1},
  templateName: {fontSize: 16, fontWeight: '700', marginTop: 1},
  changeBadge: {paddingHorizontal: 10, paddingVertical: 6, borderRadius: 8},

  // Sections
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 14,
    borderRadius: 14,
    borderWidth: 1,
  },
  sectionIconWrap: {width: 38, height: 38, borderRadius: 10, alignItems: 'center', justifyContent: 'center'},
  sectionIcon: {fontSize: 18},
  sectionTitle: {fontSize: 15, fontWeight: '700'},
  sectionHint: {fontSize: 11, fontWeight: '500', marginTop: 1},
  chevron: {fontSize: 10, fontWeight: '600'},

  sectionContent: {
    marginTop: -4,
    paddingTop: 20,
    paddingHorizontal: 16,
    paddingBottom: 16,
    borderRadius: 14,
    borderWidth: 1,
    borderTopLeftRadius: 0,
    borderTopRightRadius: 0,
    gap: 4,
  },

  // Fields
  fieldGroup: {},
  fieldGroupSpaced: {marginTop: 12},
  fieldLabelRow: {flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 6},
  fieldIcon: {fontSize: 13},
  fieldLabel: {fontSize: 12, fontWeight: '600', letterSpacing: 0.3},
  fieldInput: {
    height: 48,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 14,
    fontSize: 15,
  },

  // Summary
  summaryTip: {fontSize: 12, lineHeight: 18, fontWeight: '500', marginTop: 8},

  // Info card
  infoCard: {padding: 16, borderRadius: 14, borderWidth: 1, gap: 6},
  infoTitle: {fontSize: 14, fontWeight: '700'},
  infoText: {fontSize: 13, lineHeight: 20},

  // Empty state
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

  // Loading
  loadingContainer: {paddingHorizontal: 20, paddingTop: 20, gap: 14},
  skeletonBlock: {padding: 16, borderRadius: 14, borderWidth: 1},

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
    flexDirection: 'row',
    gap: 4,
  },
  footerBtnIcon: {fontSize: 15},
  footerBtnText: {fontSize: 12, fontWeight: '700'},
});
