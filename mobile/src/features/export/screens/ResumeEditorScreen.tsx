import React, {useState, useEffect, useCallback} from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  TextInput,
  Alert,
  ActivityIndicator,
  StatusBar,
  KeyboardAvoidingView,
  Platform,
  Modal,
  Pressable,
  FlatList,
} from 'react-native';
import Animated, {
  FadeInDown,
  FadeIn,
  FadeInRight,
  FadeOutLeft,
  SlideInRight,
  SlideOutRight,
  Layout,
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSpring,
  Easing,
} from 'react-native-reanimated';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import type {AppScreenProps} from '../../../navigation/types';
import {useTheme} from '../../../theme';
import {
  resumeEditorService,
  type ExperienceItem,
  type EducationItem,
  type CertificationItem,
} from '../services/resumeEditorService';

type Props = AppScreenProps<'ResumeEditor'>;

type SectionKey = 'experience' | 'education' | 'certifications';

interface SectionConfig {
  key: SectionKey;
  title: string;
  emoji: string;
  description: string;
  accentColor: string;
}

const SECTIONS: SectionConfig[] = [
  {
    key: 'experience',
    title: 'Experience',
    emoji: '💼',
    description: 'Work history & notable roles',
    accentColor: '#3B82F6',
  },
  {
    key: 'education',
    title: 'Education',
    emoji: '🎓',
    description: 'Degrees, schools & training',
    accentColor: '#8B5CF6',
  },
  {
    key: 'certifications',
    title: 'Licenses & Certifications',
    emoji: '🏆',
    description: 'Professional credentials',
    accentColor: '#F59E0B',
  },
];

// ─── Section Header Component ──────────────────────────────────
function SectionHeader({
  config,
  count,
  expanded,
  onToggle,
  primaryColor,
  textColor,
  subColor,
  cardColor,
  borderColor,
}: {
  config: SectionConfig;
  count: number;
  expanded: boolean;
  onToggle: () => void;
  primaryColor: string;
  textColor: string;
  subColor: string;
  cardColor: string;
  borderColor: string;
}) {
  const rotation = useSharedValue(expanded ? 1 : 0);
  useEffect(() => {
    rotation.value = withSpring(expanded ? 1 : 0, {damping: 15, stiffness: 120});
  }, [expanded, rotation]);

  const chevronStyle = useAnimatedStyle(() => ({
    transform: [{rotateZ: `${rotation.value * 90}deg`}],
  }));

  return (
    <TouchableOpacity
      activeOpacity={0.7}
      onPress={onToggle}
      style={[
        styles.sectionHeaderCard,
        {
          backgroundColor: cardColor,
          borderColor: expanded ? config.accentColor + '50' : borderColor,
          borderLeftWidth: 3,
          borderLeftColor: config.accentColor,
        },
      ]}>
      <View style={styles.sectionHeaderContent}>
        <View style={[styles.sectionEmoji, {backgroundColor: config.accentColor + '15'}]}>
          <Text style={{fontSize: 22}}>{config.emoji}</Text>
        </View>
        <View style={{flex: 1}}>
          <Text style={[styles.sectionTitle, {color: textColor}]}>{config.title}</Text>
          <Text style={[styles.sectionDescription, {color: subColor}]}>{config.description}</Text>
        </View>
        <View style={styles.sectionRight}>
          {count > 0 && (
            <View style={[styles.countBadge, {backgroundColor: config.accentColor + '20'}]}>
              <Text style={[styles.countText, {color: config.accentColor}]}>{count}</Text>
            </View>
          )}
          <Animated.View style={chevronStyle}>
            <Text style={{color: subColor, fontSize: 18, fontWeight: '300'}}>›</Text>
          </Animated.View>
        </View>
      </View>
    </TouchableOpacity>
  );
}

// ─── Month/Year Picker ──────────────────────────────────────
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];
const MONTHS_SHORT = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

function generateYears() {
  const current = new Date().getFullYear();
  const years: number[] = [];
  for (let y = current + 5; y >= 1970; y--) years.push(y);
  return years;
}
const YEARS = generateYears();

function MonthYearPicker({
  visible,
  initialValue,
  onSelect,
  onCancel,
  accentColor,
}: {
  visible: boolean;
  initialValue: string; // 'YYYY-MM' or ''
  onSelect: (value: string) => void;
  onCancel: () => void;
  accentColor: string;
}) {
  const parsed = initialValue ? {month: parseInt(initialValue.split('-')[1], 10) - 1, year: parseInt(initialValue.split('-')[0], 10)} : {month: new Date().getMonth(), year: new Date().getFullYear()};
  const [selectedMonth, setSelectedMonth] = useState(parsed.month);
  const [selectedYear, setSelectedYear] = useState(parsed.year);

  useEffect(() => {
    if (visible) {
      const p = initialValue ? {month: parseInt(initialValue.split('-')[1], 10) - 1, year: parseInt(initialValue.split('-')[0], 10)} : {month: new Date().getMonth(), year: new Date().getFullYear()};
      setSelectedMonth(p.month);
      setSelectedYear(p.year);
    }
  }, [visible, initialValue]);

  const handleConfirm = () => {
    const mm = String(selectedMonth + 1).padStart(2, '0');
    onSelect(`${selectedYear}-${mm}`);
  };

  return (
    <Modal visible={visible} transparent animationType="slide">
      <Pressable style={pickerStyles.overlay} onPress={onCancel}>
        <Pressable style={pickerStyles.sheet} onPress={e => e.stopPropagation()}>
          <View style={pickerStyles.handle} />
          <Text style={pickerStyles.title}>Select Date</Text>

          <View style={pickerStyles.columns}>
            {/* Month column */}
            <View style={pickerStyles.column}>
              <Text style={pickerStyles.colLabel}>MONTH</Text>
              <FlatList
                data={MONTHS}
                keyExtractor={(_, i) => `m${i}`}
                showsVerticalScrollIndicator={false}
                style={pickerStyles.list}
                initialScrollIndex={Math.max(0, selectedMonth - 2)}
                getItemLayout={(_, i) => ({length: 44, offset: 44 * i, index: i})}
                renderItem={({item, index}) => (
                  <TouchableOpacity
                    onPress={() => setSelectedMonth(index)}
                    style={[
                      pickerStyles.option,
                      selectedMonth === index && {backgroundColor: accentColor + '20'},
                    ]}>
                    <Text style={[
                      pickerStyles.optionText,
                      selectedMonth === index && {color: accentColor, fontWeight: '700'},
                    ]}>{item}</Text>
                  </TouchableOpacity>
                )}
              />
            </View>

            {/* Year column */}
            <View style={pickerStyles.column}>
              <Text style={pickerStyles.colLabel}>YEAR</Text>
              <FlatList
                data={YEARS}
                keyExtractor={y => `y${y}`}
                showsVerticalScrollIndicator={false}
                style={pickerStyles.list}
                initialScrollIndex={Math.max(0, YEARS.indexOf(selectedYear) - 2)}
                getItemLayout={(_, i) => ({length: 44, offset: 44 * i, index: i})}
                renderItem={({item}) => (
                  <TouchableOpacity
                    onPress={() => setSelectedYear(item)}
                    style={[
                      pickerStyles.option,
                      selectedYear === item && {backgroundColor: accentColor + '20'},
                    ]}>
                    <Text style={[
                      pickerStyles.optionText,
                      selectedYear === item && {color: accentColor, fontWeight: '700'},
                    ]}>{item}</Text>
                  </TouchableOpacity>
                )}
              />
            </View>
          </View>

          <View style={pickerStyles.actions}>
            <TouchableOpacity onPress={onCancel} style={pickerStyles.cancelBtn}>
              <Text style={pickerStyles.cancelText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={handleConfirm} style={[pickerStyles.confirmBtn, {backgroundColor: accentColor}]}>
              <Text style={pickerStyles.confirmText}>Select {MONTHS_SHORT[selectedMonth]} {selectedYear}</Text>
            </TouchableOpacity>
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function YearPicker({
  visible,
  initialValue,
  onSelect,
  onCancel,
  accentColor,
}: {
  visible: boolean;
  initialValue: string; // 'YYYY' or ''
  onSelect: (value: string) => void;
  onCancel: () => void;
  accentColor: string;
}) {
  const [selectedYear, setSelectedYear] = useState(
    initialValue ? parseInt(initialValue, 10) : new Date().getFullYear(),
  );

  useEffect(() => {
    if (visible) {
      setSelectedYear(initialValue ? parseInt(initialValue, 10) : new Date().getFullYear());
    }
  }, [visible, initialValue]);

  return (
    <Modal visible={visible} transparent animationType="slide">
      <Pressable style={pickerStyles.overlay} onPress={onCancel}>
        <Pressable style={pickerStyles.sheet} onPress={e => e.stopPropagation()}>
          <View style={pickerStyles.handle} />
          <Text style={pickerStyles.title}>Select Year</Text>

          <FlatList
            data={YEARS}
            keyExtractor={y => `y${y}`}
            showsVerticalScrollIndicator={false}
            style={[pickerStyles.list, {maxHeight: 260}]}
            initialScrollIndex={Math.max(0, YEARS.indexOf(selectedYear) - 3)}
            getItemLayout={(_, i) => ({length: 44, offset: 44 * i, index: i})}
            renderItem={({item}) => (
              <TouchableOpacity
                onPress={() => setSelectedYear(item)}
                style={[
                  pickerStyles.option,
                  selectedYear === item && {backgroundColor: accentColor + '20'},
                ]}>
                <Text style={[
                  pickerStyles.optionText,
                  {textAlign: 'center'},
                  selectedYear === item && {color: accentColor, fontWeight: '700'},
                ]}>{item}</Text>
              </TouchableOpacity>
            )}
          />

          <View style={pickerStyles.actions}>
            <TouchableOpacity onPress={onCancel} style={pickerStyles.cancelBtn}>
              <Text style={pickerStyles.cancelText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={() => onSelect(String(selectedYear))} style={[pickerStyles.confirmBtn, {backgroundColor: accentColor}]}>
              <Text style={pickerStyles.confirmText}>Select {selectedYear}</Text>
            </TouchableOpacity>
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

// ─── Date Selector Field ──────────────────────────────────────
function DateField({
  label,
  value,
  displayValue,
  onPress,
  textColor,
  subColor,
  borderColor,
  accentColor,
}: {
  label: string;
  value: string;
  displayValue: string;
  onPress: () => void;
  textColor: string;
  subColor: string;
  borderColor: string;
  accentColor: string;
}) {
  return (
    <View style={styles.fieldContainer}>
      <Text style={[styles.fieldLabel, {color: subColor}]}>{label}</Text>
      <TouchableOpacity
        onPress={onPress}
        activeOpacity={0.7}
        style={[
          styles.fieldInput,
          styles.dateSelector,
          {borderColor, backgroundColor: 'transparent'},
        ]}>
        <Text style={{color: value ? textColor : subColor + '60', fontSize: 15}}>
          {displayValue || 'Select date'}
        </Text>
        <Text style={{color: subColor, fontSize: 14}}>📅</Text>
      </TouchableOpacity>
    </View>
  );
}

// ─── Experience Form ──────────────────────────────────────────
function ExperienceForm({
  item,
  onSave,
  onCancel,
  textColor,
  subColor,
  cardColor,
  borderColor,
  accentColor,
}: {
  item?: ExperienceItem;
  onSave: (data: any) => void;
  onCancel: () => void;
  textColor: string;
  subColor: string;
  cardColor: string;
  borderColor: string;
  accentColor: string;
}) {
  const [title, setTitle] = useState(item?.title ?? '');
  const [company, setCompany] = useState(item?.company ?? '');
  const [description, setDescription] = useState(item?.description ?? '');
  const [startDate, setStartDate] = useState(item?.startDate?.substring(0, 7) ?? '');
  const [endDate, setEndDate] = useState(item?.endDate?.substring(0, 7) ?? '');
  const [isCurrent, setIsCurrent] = useState(item?.current ?? false);
  const [showStartPicker, setShowStartPicker] = useState(false);
  const [showEndPicker, setShowEndPicker] = useState(false);

  const formatMonth = (v: string) => {
    if (!v) return '';
    const [y, m] = v.split('-');
    return `${MONTHS_SHORT[parseInt(m, 10) - 1]} ${y}`;
  };

  const handleSave = () => {
    if (!title.trim() || !company.trim()) {
      Alert.alert('Required', 'Please enter both job title and company name.');
      return;
    }
    onSave({
      title: title.trim(),
      company: company.trim(),
      description: description.trim() || undefined,
      startDate: startDate ? startDate + '-01' : undefined,
      endDate: !isCurrent && endDate ? endDate + '-01' : undefined,
      current: isCurrent,
      displayOrder: item?.displayOrder ?? 0,
    });
  };

  return (
    <Animated.View
      entering={SlideInRight.duration(300).springify()}
      exiting={SlideOutRight.duration(200)}
      style={[styles.formCard, {backgroundColor: cardColor, borderColor}]}>
      <FormField label="Job Title" value={title} onChange={setTitle} placeholder="e.g. Senior Software Engineer" textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
      <FormField label="Company" value={company} onChange={setCompany} placeholder="e.g. Google" textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} autoCorrect={false} />
      <FormField label="Description" value={description} onChange={setDescription} placeholder="Key responsibilities & achievements..." multiline textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
      <View style={styles.dateRow}>
        <View style={{flex: 1}}>
          <DateField label="Start Date" value={startDate} displayValue={formatMonth(startDate)} onPress={() => setShowStartPicker(true)} textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
        </View>
        <View style={{flex: 1}}>
          {!isCurrent && (
            <DateField label="End Date" value={endDate} displayValue={formatMonth(endDate)} onPress={() => setShowEndPicker(true)} textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
          )}
        </View>
      </View>
      <TouchableOpacity onPress={() => setIsCurrent(!isCurrent)} style={styles.checkboxRow}>
        <View style={[styles.checkbox, {borderColor: accentColor}, isCurrent && {backgroundColor: accentColor}]}>
          {isCurrent && <Text style={{color: '#fff', fontSize: 12, fontWeight: '700'}}>✓</Text>}
        </View>
        <Text style={[styles.checkboxLabel, {color: textColor}]}>I currently work here</Text>
      </TouchableOpacity>
      <FormActions onSave={handleSave} onCancel={onCancel} accentColor={accentColor} isNew={!item} />
      <MonthYearPicker visible={showStartPicker} initialValue={startDate} onSelect={v => { setStartDate(v); setShowStartPicker(false); }} onCancel={() => setShowStartPicker(false)} accentColor={accentColor} />
      <MonthYearPicker visible={showEndPicker} initialValue={endDate} onSelect={v => { setEndDate(v); setShowEndPicker(false); }} onCancel={() => setShowEndPicker(false)} accentColor={accentColor} />
    </Animated.View>
  );
}

// ─── Education Form ──────────────────────────────────────────
function EducationForm({
  item,
  onSave,
  onCancel,
  textColor,
  subColor,
  cardColor,
  borderColor,
  accentColor,
}: {
  item?: EducationItem;
  onSave: (data: any) => void;
  onCancel: () => void;
  textColor: string;
  subColor: string;
  cardColor: string;
  borderColor: string;
  accentColor: string;
}) {
  const [institution, setInstitution] = useState(item?.institution ?? '');
  const [degree, setDegree] = useState(item?.degree ?? '');
  const [field, setField] = useState(item?.field ?? '');
  const [startYear, setStartYear] = useState(item?.startYear?.toString() ?? '');
  const [endYear, setEndYear] = useState(item?.endYear?.toString() ?? '');
  const [showStartPicker, setShowStartPicker] = useState(false);
  const [showEndPicker, setShowEndPicker] = useState(false);

  const handleSave = () => {
    if (!institution.trim()) {
      Alert.alert('Required', 'Please enter the institution name.');
      return;
    }
    onSave({
      institution: institution.trim(),
      degree: degree.trim() || undefined,
      field: field.trim() || undefined,
      startYear: startYear ? parseInt(startYear, 10) : undefined,
      endYear: endYear ? parseInt(endYear, 10) : undefined,
      displayOrder: item?.displayOrder ?? 0,
    });
  };

  return (
    <Animated.View
      entering={SlideInRight.duration(300).springify()}
      exiting={SlideOutRight.duration(200)}
      style={[styles.formCard, {backgroundColor: cardColor, borderColor}]}>
      <FormField label="Institution" value={institution} onChange={setInstitution} placeholder="e.g. MIT" textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} autoCorrect={false} />
      <FormField label="Degree" value={degree} onChange={setDegree} placeholder="e.g. Bachelor of Science" textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
      <FormField label="Field of Study" value={field} onChange={setField} placeholder="e.g. Computer Science" textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
      <View style={styles.dateRow}>
        <View style={{flex: 1}}>
          <DateField label="Start Year" value={startYear} displayValue={startYear || ''} onPress={() => setShowStartPicker(true)} textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
        </View>
        <View style={{flex: 1}}>
          <DateField label="End Year" value={endYear} displayValue={endYear || ''} onPress={() => setShowEndPicker(true)} textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
        </View>
      </View>
      <FormActions onSave={handleSave} onCancel={onCancel} accentColor={accentColor} isNew={!item} />
      <YearPicker visible={showStartPicker} initialValue={startYear} onSelect={v => { setStartYear(v); setShowStartPicker(false); }} onCancel={() => setShowStartPicker(false)} accentColor={accentColor} />
      <YearPicker visible={showEndPicker} initialValue={endYear} onSelect={v => { setEndYear(v); setShowEndPicker(false); }} onCancel={() => setShowEndPicker(false)} accentColor={accentColor} />
    </Animated.View>
  );
}

// ─── Certification Form ──────────────────────────────────────
function CertificationForm({
  item,
  onSave,
  onCancel,
  textColor,
  subColor,
  cardColor,
  borderColor,
  accentColor,
}: {
  item?: CertificationItem;
  onSave: (data: any) => void;
  onCancel: () => void;
  textColor: string;
  subColor: string;
  cardColor: string;
  borderColor: string;
  accentColor: string;
}) {
  const [name, setName] = useState(item?.name ?? '');
  const [org, setOrg] = useState(item?.issuingOrganization ?? '');
  const [issueDate, setIssueDate] = useState(item?.issueDate?.substring(0, 7) ?? '');
  const [expiryDate, setExpiryDate] = useState(item?.expiryDate?.substring(0, 7) ?? '');
  const [credentialId, setCredentialId] = useState(item?.credentialId ?? '');
  const [credentialUrl, setCredentialUrl] = useState(item?.credentialUrl ?? '');
  const [showIssuePicker, setShowIssuePicker] = useState(false);
  const [showExpiryPicker, setShowExpiryPicker] = useState(false);

  const formatMonth = (v: string) => {
    if (!v) return '';
    const [y, m] = v.split('-');
    return `${MONTHS_SHORT[parseInt(m, 10) - 1]} ${y}`;
  };

  const handleSave = () => {
    if (!name.trim()) {
      Alert.alert('Required', 'Please enter the certification name.');
      return;
    }
    onSave({
      name: name.trim(),
      issuingOrganization: org.trim() || undefined,
      issueDate: issueDate ? issueDate + '-01' : undefined,
      expiryDate: expiryDate ? expiryDate + '-01' : undefined,
      credentialId: credentialId.trim() || undefined,
      credentialUrl: credentialUrl.trim() || undefined,
      displayOrder: item?.displayOrder ?? 0,
    });
  };

  return (
    <Animated.View
      entering={SlideInRight.duration(300).springify()}
      exiting={SlideOutRight.duration(200)}
      style={[styles.formCard, {backgroundColor: cardColor, borderColor}]}>
      <FormField label="Certification Name" value={name} onChange={setName} placeholder="e.g. AWS Solutions Architect" textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
      <FormField label="Issuing Organization" value={org} onChange={setOrg} placeholder="e.g. Amazon Web Services" textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} autoCorrect={false} />
      <View style={styles.dateRow}>
        <View style={{flex: 1}}>
          <DateField label="Issue Date" value={issueDate} displayValue={formatMonth(issueDate)} onPress={() => setShowIssuePicker(true)} textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
        </View>
        <View style={{flex: 1}}>
          <DateField label="Expiry Date" value={expiryDate} displayValue={formatMonth(expiryDate)} onPress={() => setShowExpiryPicker(true)} textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} />
        </View>
      </View>
      <FormField label="Credential ID" value={credentialId} onChange={setCredentialId} placeholder="Optional" textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} autoCorrect={false} />
      <FormField label="Credential URL" value={credentialUrl} onChange={setCredentialUrl} placeholder="https://..." textColor={textColor} subColor={subColor} borderColor={borderColor} accentColor={accentColor} autoCorrect={false} autoCapitalize="none" />
      <FormActions onSave={handleSave} onCancel={onCancel} accentColor={accentColor} isNew={!item} />
      <MonthYearPicker visible={showIssuePicker} initialValue={issueDate} onSelect={v => { setIssueDate(v); setShowIssuePicker(false); }} onCancel={() => setShowIssuePicker(false)} accentColor={accentColor} />
      <MonthYearPicker visible={showExpiryPicker} initialValue={expiryDate} onSelect={v => { setExpiryDate(v); setShowExpiryPicker(false); }} onCancel={() => setShowExpiryPicker(false)} accentColor={accentColor} />
    </Animated.View>
  );
}

// ─── Shared Form Components ──────────────────────────────────
function FormField({
  label,
  value,
  onChange,
  placeholder,
  multiline,
  textColor,
  subColor,
  borderColor,
  accentColor,
  autoCorrect,
  autoCapitalize,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder: string;
  multiline?: boolean;
  textColor: string;
  subColor: string;
  borderColor: string;
  accentColor: string;
  autoCorrect?: boolean;
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
}) {
  const [focused, setFocused] = useState(false);
  return (
    <View style={styles.fieldContainer}>
      <Text style={[styles.fieldLabel, {color: focused ? accentColor : subColor}]}>{label}</Text>
      <TextInput
        value={value}
        onChangeText={onChange}
        placeholder={placeholder}
        placeholderTextColor={subColor + '60'}
        multiline={multiline}
        numberOfLines={multiline ? 4 : 1}
        textAlignVertical={multiline ? 'top' : 'center'}
        autoCorrect={autoCorrect}
        autoCapitalize={autoCapitalize}
        style={[
          styles.fieldInput,
          {
            color: textColor,
            borderColor: focused ? accentColor : borderColor,
            backgroundColor: focused ? accentColor + '08' : 'transparent',
          },
          multiline && {minHeight: 100, paddingTop: 12},
        ]}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
      />
    </View>
  );
}

function FormActions({
  onSave,
  onCancel,
  accentColor,
  isNew,
}: {
  onSave: () => void;
  onCancel: () => void;
  accentColor: string;
  isNew: boolean;
}) {
  return (
    <View style={styles.formActions}>
      <TouchableOpacity onPress={onCancel} style={styles.cancelBtn} activeOpacity={0.7}>
        <Text style={[styles.cancelText, {color: '#A1A1AA'}]}>Cancel</Text>
      </TouchableOpacity>
      <TouchableOpacity
        onPress={onSave}
        style={[styles.saveBtn, {backgroundColor: accentColor}]}
        activeOpacity={0.8}>
        <Text style={styles.saveBtnText}>{isNew ? '＋ Add' : '✓ Save'}</Text>
      </TouchableOpacity>
    </View>
  );
}

// ─── Item Card Components ──────────────────────────────────────────

function ExperienceCard({
  item,
  onEdit,
  onDelete,
  textColor,
  subColor,
  borderColor,
  accentColor,
}: {
  item: ExperienceItem;
  onEdit: () => void;
  onDelete: () => void;
  textColor: string;
  subColor: string;
  borderColor: string;
  accentColor: string;
}) {
  const formatDate = (d?: string) => {
    if (!d) return '';
    const date = new Date(d);
    return date.toLocaleDateString('en-US', {month: 'short', year: 'numeric'});
  };
  const dates = item.startDate
    ? `${formatDate(item.startDate)} – ${item.current ? 'Present' : formatDate(item.endDate)}`
    : '';

  return (
    <Animated.View entering={FadeInDown.duration(300).springify()} layout={Layout.springify()}>
      <View style={[styles.itemCard, {borderColor, borderLeftColor: accentColor}]}>
        <View style={styles.itemCardHeader}>
          <View style={{flex: 1}}>
            <Text style={[styles.itemTitle, {color: textColor}]}>{item.title}</Text>
            <Text style={[styles.itemSubtitle, {color: accentColor}]}>{item.company}</Text>
            {dates ? <Text style={[styles.itemDates, {color: subColor}]}>{dates}</Text> : null}
          </View>
          <View style={styles.itemActions}>
            <TouchableOpacity onPress={onEdit} hitSlop={8}>
              <Text style={{fontSize: 16}}>✏️</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={onDelete} hitSlop={8}>
              <Text style={{fontSize: 16}}>🗑️</Text>
            </TouchableOpacity>
          </View>
        </View>
        {item.description ? (
          <Text style={[styles.itemDescription, {color: subColor}]} numberOfLines={3}>
            {item.description}
          </Text>
        ) : null}
      </View>
    </Animated.View>
  );
}

function EducationCard({
  item,
  onEdit,
  onDelete,
  textColor,
  subColor,
  borderColor,
  accentColor,
}: {
  item: EducationItem;
  onEdit: () => void;
  onDelete: () => void;
  textColor: string;
  subColor: string;
  borderColor: string;
  accentColor: string;
}) {
  const deg = [item.degree, item.field].filter(Boolean).join(' in ');
  const years = [item.startYear, item.endYear].filter(Boolean).join(' – ');

  return (
    <Animated.View entering={FadeInDown.duration(300).springify()} layout={Layout.springify()}>
      <View style={[styles.itemCard, {borderColor, borderLeftColor: accentColor}]}>
        <View style={styles.itemCardHeader}>
          <View style={{flex: 1}}>
            <Text style={[styles.itemTitle, {color: textColor}]}>{deg || item.institution}</Text>
            {deg ? <Text style={[styles.itemSubtitle, {color: accentColor}]}>{item.institution}</Text> : null}
            {years ? <Text style={[styles.itemDates, {color: subColor}]}>{years}</Text> : null}
          </View>
          <View style={styles.itemActions}>
            <TouchableOpacity onPress={onEdit} hitSlop={8}>
              <Text style={{fontSize: 16}}>✏️</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={onDelete} hitSlop={8}>
              <Text style={{fontSize: 16}}>🗑️</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Animated.View>
  );
}

function CertificationCard({
  item,
  onEdit,
  onDelete,
  textColor,
  subColor,
  borderColor,
  accentColor,
}: {
  item: CertificationItem;
  onEdit: () => void;
  onDelete: () => void;
  textColor: string;
  subColor: string;
  borderColor: string;
  accentColor: string;
}) {
  const formatDate = (d?: string) => {
    if (!d) return '';
    const date = new Date(d);
    return date.toLocaleDateString('en-US', {month: 'short', year: 'numeric'});
  };

  return (
    <Animated.View entering={FadeInDown.duration(300).springify()} layout={Layout.springify()}>
      <View style={[styles.itemCard, {borderColor, borderLeftColor: accentColor}]}>
        <View style={styles.itemCardHeader}>
          <View style={{flex: 1}}>
            <Text style={[styles.itemTitle, {color: textColor}]}>{item.name}</Text>
            {item.issuingOrganization ? (
              <Text style={[styles.itemSubtitle, {color: accentColor}]}>{item.issuingOrganization}</Text>
            ) : null}
            {item.issueDate ? (
              <Text style={[styles.itemDates, {color: subColor}]}>
                Issued {formatDate(item.issueDate)}
                {item.expiryDate ? ` · Expires ${formatDate(item.expiryDate)}` : ''}
              </Text>
            ) : null}
            {item.credentialId ? (
              <Text style={[styles.itemCred, {color: subColor}]}>ID: {item.credentialId}</Text>
            ) : null}
          </View>
          <View style={styles.itemActions}>
            <TouchableOpacity onPress={onEdit} hitSlop={8}>
              <Text style={{fontSize: 16}}>✏️</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={onDelete} hitSlop={8}>
              <Text style={{fontSize: 16}}>🗑️</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Animated.View>
  );
}

// ─── Empty State ──────────────────────────────────────────────
function EmptyState({
  emoji,
  title,
  description,
  accentColor,
}: {
  emoji: string;
  title: string;
  description: string;
  accentColor: string;
}) {
  return (
    <Animated.View entering={FadeIn.duration(400)} style={styles.emptyState}>
      <View style={[styles.emptyEmoji, {backgroundColor: accentColor + '12'}]}>
        <Text style={{fontSize: 32}}>{emoji}</Text>
      </View>
      <Text style={[styles.emptyTitle, {color: accentColor}]}>{title}</Text>
      <Text style={[styles.emptyDesc, {color: '#A1A1AA'}]}>{description}</Text>
    </Animated.View>
  );
}

// ─── Main Screen ──────────────────────────────────────────────
export default function ResumeEditorScreen({navigation, route}: Props) {
  const {portfolioId} = route.params;
  const {theme} = useTheme();
  const insets = useSafeAreaInsets();

  // Data state
  const [experiences, setExperiences] = useState<ExperienceItem[]>([]);
  const [educations, setEducations] = useState<EducationItem[]>([]);
  const [certifications, setCertifications] = useState<CertificationItem[]>([]);
  const [loading, setLoading] = useState(true);

  // UI state
  const [expandedSection, setExpandedSection] = useState<SectionKey | null>(null);
  const [editingItem, setEditingItem] = useState<{section: SectionKey; item?: any} | null>(null);
  const [saving, setSaving] = useState(false);

  // ── Load data ──────────────────────
  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [exp, edu, cert] = await Promise.all([
        resumeEditorService.listExperiences(portfolioId),
        resumeEditorService.listEducation(portfolioId),
        resumeEditorService.listCertifications(portfolioId),
      ]);
      setExperiences(exp);
      setEducations(edu);
      setCertifications(cert);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  }, [portfolioId]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  // ── Section toggle ─────────────────
  const toggleSection = (key: SectionKey) => {
    setEditingItem(null);
    setExpandedSection(prev => (prev === key ? null : key));
  };

  // ── CRUD handlers ──────────────────
  const handleSaveExperience = async (data: any) => {
    setSaving(true);
    try {
      if (editingItem?.item) {
        await resumeEditorService.updateExperience(editingItem.item.id, data);
      } else {
        await resumeEditorService.createExperience(portfolioId, data);
      }
      setEditingItem(null);
      const updated = await resumeEditorService.listExperiences(portfolioId);
      setExperiences(updated);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  const handleSaveEducation = async (data: any) => {
    setSaving(true);
    try {
      if (editingItem?.item) {
        await resumeEditorService.updateEducation(editingItem.item.id, data);
      } else {
        await resumeEditorService.createEducation(portfolioId, data);
      }
      setEditingItem(null);
      const updated = await resumeEditorService.listEducation(portfolioId);
      setEducations(updated);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  const handleSaveCertification = async (data: any) => {
    setSaving(true);
    try {
      if (editingItem?.item) {
        await resumeEditorService.updateCertification(editingItem.item.id, data);
      } else {
        await resumeEditorService.createCertification(portfolioId, data);
      }
      setEditingItem(null);
      const updated = await resumeEditorService.listCertifications(portfolioId);
      setCertifications(updated);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (section: SectionKey, id: number) => {
    Alert.alert('Delete', 'Are you sure you want to remove this item?', [
      {text: 'Cancel', style: 'cancel'},
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          try {
            if (section === 'experience') {
              await resumeEditorService.deleteExperience(id);
              setExperiences(prev => prev.filter(e => e.id !== id));
            } else if (section === 'education') {
              await resumeEditorService.deleteEducation(id);
              setEducations(prev => prev.filter(e => e.id !== id));
            } else {
              await resumeEditorService.deleteCertification(id);
              setCertifications(prev => prev.filter(c => c.id !== id));
            }
          } catch (err: any) {
            Alert.alert('Error', err.message || 'Failed to delete');
          }
        },
      },
    ]);
  };

  // ── Render helpers ──────────────────
  const getCount = (key: SectionKey) => {
    switch (key) {
      case 'experience':
        return experiences.length;
      case 'education':
        return educations.length;
      case 'certifications':
        return certifications.length;
    }
  };

  const renderSectionContent = (key: SectionKey) => {
    const {colors} = theme;
    const sectionConfig = SECTIONS.find(s => s.key === key)!;
    const accent = sectionConfig.accentColor;

    // Show form if adding/editing
    if (editingItem?.section === key) {
      switch (key) {
        case 'experience':
          return <ExperienceForm item={editingItem.item} onSave={handleSaveExperience} onCancel={() => setEditingItem(null)} textColor={colors.text} subColor={colors.textSecondary} cardColor={colors.card ?? '#16161E'} borderColor={colors.border} accentColor={accent} />;
        case 'education':
          return <EducationForm item={editingItem.item} onSave={handleSaveEducation} onCancel={() => setEditingItem(null)} textColor={colors.text} subColor={colors.textSecondary} cardColor={colors.card ?? '#16161E'} borderColor={colors.border} accentColor={accent} />;
        case 'certifications':
          return <CertificationForm item={editingItem.item} onSave={handleSaveCertification} onCancel={() => setEditingItem(null)} textColor={colors.text} subColor={colors.textSecondary} cardColor={colors.card ?? '#16161E'} borderColor={colors.border} accentColor={accent} />;
      }
    }

    // Show items list
    const items = key === 'experience' ? experiences : key === 'education' ? educations : certifications;
    const emptyMessages = {
      experience: {title: 'No experience yet', desc: 'Add your work history to make your resume stand out'},
      education: {title: 'No education yet', desc: 'Add your degrees and academic achievements'},
      certifications: {title: 'No certifications yet', desc: 'Add professional credentials to boost credibility'},
    };

    return (
      <Animated.View entering={FadeInDown.duration(300)} style={styles.sectionContent}>
        {items.length === 0 ? (
          <EmptyState
            emoji={sectionConfig.emoji}
            title={emptyMessages[key].title}
            description={emptyMessages[key].desc}
            accentColor={accent}
          />
        ) : (
          <View style={{gap: 10}}>
            {key === 'experience' && experiences.map(exp => (
              <ExperienceCard key={exp.id} item={exp} onEdit={() => setEditingItem({section: key, item: exp})} onDelete={() => handleDelete(key, exp.id)} textColor={colors.text} subColor={colors.textSecondary} borderColor={colors.border} accentColor={accent} />
            ))}
            {key === 'education' && educations.map(edu => (
              <EducationCard key={edu.id} item={edu} onEdit={() => setEditingItem({section: key, item: edu})} onDelete={() => handleDelete(key, edu.id)} textColor={colors.text} subColor={colors.textSecondary} borderColor={colors.border} accentColor={accent} />
            ))}
            {key === 'certifications' && certifications.map(cert => (
              <CertificationCard key={cert.id} item={cert} onEdit={() => setEditingItem({section: key, item: cert})} onDelete={() => handleDelete(key, cert.id)} textColor={colors.text} subColor={colors.textSecondary} borderColor={colors.border} accentColor={accent} />
            ))}
          </View>
        )}
        <TouchableOpacity
          onPress={() => setEditingItem({section: key})}
          style={[styles.addBtn, {borderColor: accent + '40', backgroundColor: accent + '08'}]}
          activeOpacity={0.7}>
          <Text style={[styles.addBtnText, {color: accent}]}>＋ Add {key === 'experience' ? 'Experience' : key === 'education' ? 'Education' : 'Certification'}</Text>
        </TouchableOpacity>
      </Animated.View>
    );
  };

  // ── RENDER ─────────────────────────
  const {colors} = theme;

  if (loading) {
    return (
      <View style={[styles.container, {backgroundColor: colors.background}]}>
        <StatusBar barStyle="light-content" />
        <View style={[styles.loadingContainer, {paddingTop: insets.top + 60}]}>
          <ActivityIndicator color={colors.primary} size="large" />
          <Text style={[styles.loadingText, {color: colors.textSecondary}]}>Loading your resume data...</Text>
        </View>
      </View>
    );
  }

  return (
    <View style={[styles.container, {backgroundColor: colors.background}]}>
      <StatusBar barStyle="light-content" />

      {/* ── Header ── */}
      <View style={[styles.header, {paddingTop: insets.top + 8, borderBottomColor: colors.border}]}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={12}>
          <Text style={[styles.backText, {color: colors.text}]}>‹</Text>
        </TouchableOpacity>
        <View style={{flex: 1}}>
          <Text style={[styles.headerTitle, {color: colors.text}]}>Edit Resume</Text>
          <Text style={[styles.headerSub, {color: colors.textSecondary}]}>
            {experiences.length + educations.length + certifications.length} items total
          </Text>
        </View>
      </View>

      {/* ── Content ── */}
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={{flex: 1}}>
        <ScrollView
          contentContainerStyle={[styles.scrollContent, {paddingBottom: insets.bottom + 30}]}
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled">

          {/* Hero tip */}
          <Animated.View entering={FadeInDown.delay(100).duration(400).springify()}>
            <View style={[styles.heroTip, {backgroundColor: colors.primary + '10', borderColor: colors.primary + '25'}]}>
              <Text style={{fontSize: 20}}>✨</Text>
              <View style={{flex: 1}}>
                <Text style={[styles.heroTipTitle, {color: colors.primary}]}>
                  Make your resume shine
                </Text>
                <Text style={[styles.heroTipDesc, {color: colors.textSecondary}]}>
                  Add your education, work experience, and certifications. These sections appear on your exported PDF resume.
                </Text>
              </View>
            </View>
          </Animated.View>

          {/* Sections */}
          {SECTIONS.map((section, index) => (
            <Animated.View
              key={section.key}
              entering={FadeInDown.delay(150 + index * 80)
                .duration(400)
                .springify()}>
              <SectionHeader
                config={section}
                count={getCount(section.key)}
                expanded={expandedSection === section.key}
                onToggle={() => toggleSection(section.key)}
                primaryColor={colors.primary}
                textColor={colors.text}
                subColor={colors.textSecondary}
                cardColor={colors.card ?? '#16161E'}
                borderColor={colors.border}
              />
              {expandedSection === section.key && renderSectionContent(section.key)}
            </Animated.View>
          ))}

          {/* Done button */}
          <Animated.View entering={FadeInDown.delay(400).duration(400).springify()}>
            <TouchableOpacity
              onPress={() => navigation.goBack()}
              activeOpacity={0.8}
              style={[styles.doneBtn, {backgroundColor: colors.primary}]}>
              <Text style={{fontSize: 16}}>✓</Text>
              <Text style={styles.doneBtnText}>Done Editing</Text>
            </TouchableOpacity>
          </Animated.View>
        </ScrollView>
      </KeyboardAvoidingView>

      {/* Saving overlay */}
      {saving && (
        <Animated.View entering={FadeIn.duration(200)} style={styles.savingOverlay}>
          <ActivityIndicator color="#fff" size="small" />
          <Text style={styles.savingText}>Saving...</Text>
        </Animated.View>
      )}
    </View>
  );
}

// ─── Styles ──────────────────────────────────────────────────
const styles = StyleSheet.create({
  container: {flex: 1},

  // Loading
  loadingContainer: {flex: 1, alignItems: 'center', justifyContent: 'center', gap: 16},
  loadingText: {fontSize: 14, fontWeight: '500'},

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

  // Scroll content
  scrollContent: {paddingHorizontal: 20, paddingTop: 16, gap: 14},

  // Hero tip
  heroTip: {
    flexDirection: 'row',
    padding: 16,
    borderRadius: 14,
    borderWidth: 1,
    gap: 12,
    alignItems: 'flex-start',
  },
  heroTipTitle: {fontSize: 15, fontWeight: '700', marginBottom: 3},
  heroTipDesc: {fontSize: 13, lineHeight: 19},

  // Section header
  sectionHeaderCard: {
    borderRadius: 14,
    borderWidth: 1,
    padding: 16,
    overflow: 'hidden',
  },
  sectionHeaderContent: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
  },
  sectionEmoji: {
    width: 44,
    height: 44,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sectionTitle: {fontSize: 16, fontWeight: '700', letterSpacing: -0.3},
  sectionDescription: {fontSize: 12, marginTop: 2},
  sectionRight: {flexDirection: 'row', alignItems: 'center', gap: 8},
  countBadge: {
    minWidth: 24,
    height: 24,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 7,
  },
  countText: {fontSize: 12, fontWeight: '800'},

  // Section content
  sectionContent: {paddingTop: 14, paddingBottom: 4, gap: 10, marginTop: 2},

  // Date selector
  dateSelector: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },

  // Item cards
  itemCard: {
    borderWidth: 1,
    borderLeftWidth: 3,
    borderRadius: 12,
    padding: 14,
  },
  itemCardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  itemTitle: {fontSize: 15, fontWeight: '700'},
  itemSubtitle: {fontSize: 13, fontWeight: '600', marginTop: 2},
  itemDates: {fontSize: 12, marginTop: 3},
  itemDescription: {fontSize: 13, lineHeight: 19, marginTop: 8},
  itemCred: {fontSize: 11, marginTop: 3, fontStyle: 'italic'},
  itemActions: {flexDirection: 'row', gap: 12, paddingLeft: 8, paddingTop: 2},

  // Forms
  formCard: {borderWidth: 1, borderRadius: 14, padding: 18, gap: 14},
  dateRow: {flexDirection: 'row', gap: 12},
  fieldContainer: {gap: 6},
  fieldLabel: {fontSize: 12, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1},
  fieldInput: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 15,
  },
  checkboxRow: {flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 4},
  checkbox: {
    width: 22,
    height: 22,
    borderRadius: 6,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxLabel: {fontSize: 14, fontWeight: '500'},
  formActions: {flexDirection: 'row', justifyContent: 'flex-end', gap: 12, marginTop: 4},
  cancelBtn: {paddingVertical: 10, paddingHorizontal: 18},
  cancelText: {fontSize: 14, fontWeight: '600'},
  saveBtn: {paddingVertical: 10, paddingHorizontal: 22, borderRadius: 10},
  saveBtnText: {fontSize: 14, fontWeight: '800', color: '#fff'},

  // Add button
  addBtn: {
    borderWidth: 1,
    borderStyle: 'dashed',
    borderRadius: 12,
    padding: 14,
    alignItems: 'center',
  },
  addBtnText: {fontSize: 14, fontWeight: '700'},

  // Empty state
  emptyState: {alignItems: 'center', paddingVertical: 24, gap: 8},
  emptyEmoji: {width: 64, height: 64, borderRadius: 20, alignItems: 'center', justifyContent: 'center'},
  emptyTitle: {fontSize: 16, fontWeight: '700', marginTop: 4},
  emptyDesc: {fontSize: 13, textAlign: 'center', paddingHorizontal: 20},

  // Done button
  doneBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 16,
    borderRadius: 14,
    gap: 8,
    marginTop: 6,
  },
  doneBtnText: {fontSize: 16, fontWeight: '800', color: '#fff'},

  // Saving overlay
  savingOverlay: {
    position: 'absolute',
    bottom: 40,
    alignSelf: 'center',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: 'rgba(0,0,0,0.85)',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 100,
  },
  savingText: {color: '#fff', fontSize: 14, fontWeight: '600'},
});

// ─── Picker Styles ──────────────────────────────────────────
const pickerStyles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: '#1A1A24',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingHorizontal: 20,
    paddingBottom: 40,
    paddingTop: 12,
  },
  handle: {
    width: 36,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#333',
    alignSelf: 'center',
    marginBottom: 16,
  },
  title: {
    color: '#F4F4F5',
    fontSize: 18,
    fontWeight: '800',
    textAlign: 'center',
    marginBottom: 16,
    letterSpacing: -0.3,
  },
  columns: {
    flexDirection: 'row',
    gap: 12,
  },
  column: {
    flex: 1,
  },
  colLabel: {
    color: '#71717A',
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1,
    textTransform: 'uppercase',
    marginBottom: 8,
    textAlign: 'center',
  },
  list: {
    maxHeight: 220,
    borderRadius: 12,
    backgroundColor: '#111118',
  },
  option: {
    height: 44,
    justifyContent: 'center',
    paddingHorizontal: 16,
    borderRadius: 8,
  },
  optionText: {
    color: '#A1A1AA',
    fontSize: 15,
    fontWeight: '500',
  },
  actions: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 20,
  },
  cancelBtn: {
    flex: 1,
    paddingVertical: 14,
    alignItems: 'center',
    borderRadius: 12,
    backgroundColor: '#27272A',
  },
  cancelText: {
    color: '#A1A1AA',
    fontSize: 15,
    fontWeight: '600',
  },
  confirmBtn: {
    flex: 2,
    paddingVertical: 14,
    alignItems: 'center',
    borderRadius: 12,
  },
  confirmText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '700',
  },
});
