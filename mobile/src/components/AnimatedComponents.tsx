import React, {useEffect, useCallback} from 'react';
import {ViewStyle, Pressable, StyleSheet} from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withSpring,
  withTiming,
  withDelay,
  FadeIn,
  FadeInDown,
  FadeInUp,
  FadeInLeft,
  FadeInRight,
  FadeOut,
  SlideInRight,
  SlideOutLeft,
  ZoomIn,
  Easing,
  interpolate,
  runOnJS,
} from 'react-native-reanimated';

// ─── Spring Configs ──────────────────────────────────────────
export const SPRING_CONFIG = {
  damping: 15,
  stiffness: 150,
  mass: 0.8,
};

export const SPRING_BOUNCY = {
  damping: 12,
  stiffness: 200,
  mass: 0.6,
};

export const SPRING_GENTLE = {
  damping: 20,
  stiffness: 120,
  mass: 1,
};

// ─── Preset Animations (for entering/layout) ──────────────
export const ENTER_FADE = FadeIn.duration(400).easing(Easing.out(Easing.cubic));
export const ENTER_FADE_DOWN = FadeInDown.duration(500).springify().damping(18);
export const ENTER_FADE_UP = FadeInUp.duration(500).springify().damping(18);
export const ENTER_SLIDE_RIGHT = SlideInRight.duration(350).easing(Easing.out(Easing.cubic));
export const EXIT_FADE = FadeOut.duration(300);
export const EXIT_SLIDE_LEFT = SlideOutLeft.duration(300);
export const ENTER_ZOOM = ZoomIn.duration(400).springify().damping(15);

// ─── Stagger helper ──────────────────────────────────────────
export function staggerDelay(index: number, base = 60) {
  return FadeInDown.delay(index * base)
    .duration(450)
    .springify()
    .damping(18);
}

export function staggerFadeIn(index: number, base = 50) {
  return FadeIn.delay(index * base).duration(350);
}

// ─── FadeInView ──────────────────────────────────────────────
interface FadeInViewProps {
  children: React.ReactNode;
  delay?: number;
  duration?: number;
  style?: ViewStyle;
  translateY?: number;
}

export function FadeInView({
  children,
  delay: d = 0,
  duration = 500,
  style,
  translateY = 20,
}: FadeInViewProps) {
  const opacity = useSharedValue(0);
  const translate = useSharedValue(translateY);

  useEffect(() => {
    opacity.value = withDelay(d, withTiming(1, {duration, easing: Easing.out(Easing.cubic)}));
    translate.value = withDelay(d, withSpring(0, SPRING_CONFIG));
  }, [d, duration, opacity, translate, translateY]);

  const animStyle = useAnimatedStyle(() => ({
    opacity: opacity.value,
    transform: [{translateY: translate.value}],
  }));

  return (
    <Animated.View style={[style, animStyle]}>
      {children}
    </Animated.View>
  );
}

// ─── ScalePress ──────────────────────────────────────────────
// A pressable that scales down on press for tactile feedback
interface ScalePressProps {
  children: React.ReactNode;
  onPress?: () => void;
  disabled?: boolean;
  style?: ViewStyle;
  scaleTo?: number;
  activeOpacity?: number;
}

export function ScalePress({
  children,
  onPress,
  disabled = false,
  style,
  scaleTo = 0.97,
  activeOpacity = 0.85,
}: ScalePressProps) {
  const scale = useSharedValue(1);
  const opacityVal = useSharedValue(1);

  const animStyle = useAnimatedStyle(() => ({
    transform: [{scale: scale.value}],
    opacity: opacityVal.value,
  }));

  const handlePressIn = useCallback(() => {
    scale.value = withSpring(scaleTo, SPRING_BOUNCY);
    opacityVal.value = withTiming(activeOpacity, {duration: 100});
  }, [scale, opacityVal, scaleTo, activeOpacity]);

  const handlePressOut = useCallback(() => {
    scale.value = withSpring(1, SPRING_BOUNCY);
    opacityVal.value = withTiming(1, {duration: 150});
  }, [scale, opacityVal]);

  return (
    <Pressable
      onPress={onPress}
      onPressIn={handlePressIn}
      onPressOut={handlePressOut}
      disabled={disabled}
      style={style}>
      <Animated.View style={animStyle}>{children}</Animated.View>
    </Pressable>
  );
}

// ─── PulseView ───────────────────────────────────────────────
// Softly pulses (for loading states, empty state icons, etc.)
interface PulseViewProps {
  children: React.ReactNode;
  style?: ViewStyle;
  active?: boolean;
}

export function PulseView({children, style, active = true}: PulseViewProps) {
  const scale = useSharedValue(1);

  useEffect(() => {
    if (active) {
      const pulse = () => {
        scale.value = withSpring(1.05, SPRING_GENTLE, () => {
          scale.value = withSpring(1, SPRING_GENTLE);
        });
      };
      pulse();
      const interval = setInterval(pulse, 2000);
      return () => clearInterval(interval);
    }
  }, [active, scale]);

  const animStyle = useAnimatedStyle(() => ({
    transform: [{scale: scale.value}],
  }));

  return (
    <Animated.View style={[style, animStyle]}>{children}</Animated.View>
  );
}

// ─── Shimmer Skeleton ────────────────────────────────────────
interface SkeletonProps {
  width: number | string;
  height: number;
  borderRadius?: number;
  style?: ViewStyle;
  color?: string;
}

export function Skeleton({
  width,
  height,
  borderRadius = 8,
  style,
  color = 'rgba(255,255,255,0.06)',
}: SkeletonProps) {
  const shimmer = useSharedValue(0);

  useEffect(() => {
    const animate = () => {
      shimmer.value = withTiming(1, {duration: 1200, easing: Easing.inOut(Easing.ease)}, () => {
        shimmer.value = 0;
      });
    };
    animate();
    const interval = setInterval(animate, 1500);
    return () => clearInterval(interval);
  }, [shimmer]);

  const animStyle = useAnimatedStyle(() => ({
    opacity: interpolate(shimmer.value, [0, 0.5, 1], [0.4, 0.8, 0.4]),
  }));

  return (
    <Animated.View
      style={[
        {
          width: width as any,
          height,
          borderRadius,
          backgroundColor: color,
        },
        animStyle,
        style,
      ]}
    />
  );
}

// ─── GlowBlob ────────────────────────────────────────────────
// Animated ambient glow for hero sections
interface GlowBlobProps {
  color: string;
  size?: number;
  style?: ViewStyle;
}

export function GlowBlob({color, size = 320, style}: GlowBlobProps) {
  const scale = useSharedValue(1);
  const opacityVal = useSharedValue(0.08);

  useEffect(() => {
    const breathe = () => {
      scale.value = withTiming(1.15, {duration: 3000, easing: Easing.inOut(Easing.ease)}, () => {
        scale.value = withTiming(1, {duration: 3000, easing: Easing.inOut(Easing.ease)});
      });
      opacityVal.value = withTiming(0.12, {duration: 3000, easing: Easing.inOut(Easing.ease)}, () => {
        opacityVal.value = withTiming(0.06, {duration: 3000, easing: Easing.inOut(Easing.ease)});
      });
    };
    breathe();
    const interval = setInterval(breathe, 6000);
    return () => clearInterval(interval);
  }, [scale, opacityVal]);

  const animStyle = useAnimatedStyle(() => ({
    transform: [{scale: scale.value}],
    opacity: opacityVal.value,
  }));

  return (
    <Animated.View
      style={[
        {
          position: 'absolute',
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor: color,
          alignSelf: 'center',
        },
        animStyle,
        style,
      ]}
    />
  );
}

// ─── AnimatedNumber ──────────────────────────────────────────
// Animates a number counting up (for stats)
interface AnimatedNumberProps {
  value: number;
  style?: any;
  duration?: number;
}

export function AnimatedNumber({value, style, duration = 600}: AnimatedNumberProps) {
  const animValue = useSharedValue(0);

  useEffect(() => {
    animValue.value = withTiming(value, {duration, easing: Easing.out(Easing.cubic)});
  }, [value, animValue, duration]);

  const animStyle = useAnimatedStyle(() => ({
    // We'll use a text approach instead
  }));

  // For simplicity, just render the number directly since
  // Reanimated text needs extra setup
  return (
    <Animated.Text style={style}>{value}</Animated.Text>
  );
}

// ─── ProgressBar ─────────────────────────────────────────────
interface ProgressBarProps {
  progress: number; // 0-1
  color: string;
  backgroundColor?: string;
  height?: number;
  borderRadius?: number;
  style?: ViewStyle;
}

export function ProgressBar({
  progress,
  color,
  backgroundColor = 'rgba(255,255,255,0.06)',
  height = 4,
  borderRadius = 2,
  style,
}: ProgressBarProps) {
  const width = useSharedValue(0);

  useEffect(() => {
    width.value = withSpring(progress, SPRING_GENTLE);
  }, [progress, width]);

  const barStyle = useAnimatedStyle(() => ({
    width: `${width.value * 100}%`,
  }));

  return (
    <Animated.View style={[{height, borderRadius, backgroundColor, overflow: 'hidden'}, style]}>
      <Animated.View
        style={[
          {height, borderRadius, backgroundColor: color},
          barStyle,
        ]}
      />
    </Animated.View>
  );
}
