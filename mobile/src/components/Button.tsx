import React, {useCallback} from 'react';
import {
  ActivityIndicator,
  StyleSheet,
  Text,
  View,
  ViewStyle,
  Pressable,
} from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withSpring,
  withTiming,
} from 'react-native-reanimated';
import {useTheme} from '../theme';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

interface ButtonProps {
  label: string;
  onPress: () => void;
  variant?: ButtonVariant;
  loading?: boolean;
  disabled?: boolean;
  icon?: string;
  style?: ViewStyle;
  fullWidth?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

const SPRING = {damping: 12, stiffness: 200, mass: 0.6};

export function Button({
  label,
  onPress,
  variant = 'primary',
  loading = false,
  disabled = false,
  icon,
  style,
  fullWidth = true,
  size = 'md',
}: ButtonProps) {
  const {theme} = useTheme();
  const isDisabled = disabled || loading;

  const scale = useSharedValue(1);
  const opacityVal = useSharedValue(1);

  const animStyle = useAnimatedStyle(() => ({
    transform: [{scale: scale.value}],
    opacity: opacityVal.value,
  }));

  const handlePressIn = useCallback(() => {
    if (isDisabled) return;
    scale.value = withSpring(0.96, SPRING);
    opacityVal.value = withTiming(0.85, {duration: 80});
  }, [isDisabled, scale, opacityVal]);

  const handlePressOut = useCallback(() => {
    scale.value = withSpring(1, SPRING);
    opacityVal.value = withTiming(1, {duration: 120});
  }, [scale, opacityVal]);

  const getContainerStyle = (): ViewStyle => {
    switch (variant) {
      case 'primary':
        return {
          backgroundColor: isDisabled
            ? theme.colors.primary + '55'
            : theme.colors.primary,
          shadowColor: theme.colors.shadowColor ?? theme.colors.primary,
          shadowOffset: {width: 0, height: 4},
          shadowOpacity: isDisabled ? 0 : 0.3,
          shadowRadius: 12,
          elevation: isDisabled ? 0 : 6,
        };
      case 'secondary':
        return {
          backgroundColor: theme.colors.primary + '12',
          borderWidth: 1.5,
          borderColor: theme.colors.primary + '44',
        };
      case 'ghost':
        return {
          backgroundColor: 'rgba(255,255,255,0.04)',
        };
      case 'danger':
        return {
          backgroundColor: theme.colors.error + '15',
          borderWidth: 1.5,
          borderColor: theme.colors.error + '33',
        };
    }
  };

  const getTextColor = (): string => {
    switch (variant) {
      case 'primary':
        return '#FFFFFF';
      case 'secondary':
        return isDisabled ? theme.colors.primary + '88' : theme.colors.primary;
      case 'ghost':
        return theme.colors.textSecondary;
      case 'danger':
        return theme.colors.error;
    }
  };

  const getHeight = () => {
    switch (size) {
      case 'sm': return 40;
      case 'md': return 50;
      case 'lg': return 56;
    }
  };

  const getFontSize = () => {
    switch (size) {
      case 'sm': return 13;
      case 'md': return 15;
      case 'lg': return 16;
    }
  };

  return (
    <Pressable
      onPress={onPress}
      onPressIn={handlePressIn}
      onPressOut={handlePressOut}
      disabled={isDisabled}>
      <Animated.View
        style={[
          styles.base,
          {height: getHeight(), borderRadius: size === 'sm' ? 10 : 14},
          getContainerStyle(),
          !fullWidth && styles.inline,
          style,
          animStyle,
        ]}>
        {loading ? (
          <ActivityIndicator
            size="small"
            color={variant === 'primary' ? '#FFFFFF' : theme.colors.primary}
          />
        ) : (
          <View style={styles.row}>
            {icon ? <Text style={styles.icon}>{icon}</Text> : null}
            <Text
              style={[
                styles.label,
                {color: getTextColor(), fontSize: getFontSize()},
              ]}>
              {label}
            </Text>
          </View>
        )}
      </Animated.View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  inline: {
    alignSelf: 'flex-start',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  icon: {
    fontSize: 18,
  },
  label: {
    fontWeight: '600',
    letterSpacing: 0.2,
  },
});
