import React, {useCallback} from 'react';
import {StyleSheet, ViewStyle, Pressable} from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withSpring,
  withTiming,
} from 'react-native-reanimated';
import {useTheme} from '../theme';

const SPRING = {damping: 15, stiffness: 180, mass: 0.7};

interface CardProps {
  children: React.ReactNode;
  style?: ViewStyle;
  onPress?: () => void;
  padding?: number;
  elevated?: boolean;
  glowColor?: string;
}

export function Card({
  children,
  style,
  onPress,
  padding = 20,
  elevated = false,
  glowColor,
}: CardProps) {
  const {theme} = useTheme();
  const scale = useSharedValue(1);
  const shadowOpacity = useSharedValue(elevated ? 0.15 : 0);

  const animStyle = useAnimatedStyle(() => ({
    transform: [{scale: scale.value}],
  }));

  const handleIn = useCallback(() => {
    if (!onPress) return;
    scale.value = withSpring(0.98, SPRING);
  }, [onPress, scale]);

  const handleOut = useCallback(() => {
    if (!onPress) return;
    scale.value = withSpring(1, SPRING);
  }, [onPress, scale]);

  const cardStyle: ViewStyle = {
    backgroundColor: theme.colors.card ?? theme.colors.surface,
    borderColor: glowColor
      ? glowColor + '33'
      : theme.colors.cardBorder ?? theme.colors.border,
    padding,
    ...(elevated && {
      shadowColor: glowColor ?? theme.colors.shadowColor ?? theme.colors.primary,
      shadowOffset: {width: 0, height: 4},
      shadowOpacity: 0.15,
      shadowRadius: 16,
      elevation: 4,
    }),
  };

  if (onPress) {
    return (
      <Pressable onPress={onPress} onPressIn={handleIn} onPressOut={handleOut}>
        <Animated.View style={[styles.card, cardStyle, style, animStyle]}>
          {children}
        </Animated.View>
      </Pressable>
    );
  }

  return (
    <Animated.View style={[styles.card, cardStyle, style]}>
      {children}
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: 18,
    borderWidth: 1,
    overflow: 'hidden',
  },
});
