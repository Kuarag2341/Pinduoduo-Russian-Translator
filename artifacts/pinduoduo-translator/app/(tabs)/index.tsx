import { Feather } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useState } from 'react';
import {
  Alert,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useColors } from '@/hooks/useColors';

export default function TranslatorHome() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const textColors = colors.translatedTextColors;
  const [fontSize, setFontSize] = useState(15);
  const [textColor, setTextColor] = useState<string>(textColors[0]);
  const [previewNotice, setPreviewNotice] = useState('');
  const topInset = Platform.OS === 'web' ? Math.max(insets.top, 67) : insets.top;
  const bottomInset = Platform.OS === 'web' ? Math.max(insets.bottom, 34) : insets.bottom;

  const explainPreview = () => {
    setPreviewNotice('Захват экрана и перевод доступны в нативном Android APK. В этом предпросмотре можно оценить интерфейс.');
    Alert.alert(
      'Нужна Android-сборка',
      'Захват экрана, разрешения и перевод работают в нативном APK. Предпросмотр показывает интерфейс приложения.',
    );
  };

  return (
    <ScrollView
      style={[styles.screen, { backgroundColor: colors.background }]}
      contentContainerStyle={[
        styles.content,
        { paddingTop: topInset + 18, paddingBottom: bottomInset + 24 },
      ]}
      showsVerticalScrollIndicator={false}
    >
      <View style={styles.brandRow}>
        <View style={[styles.brandMark, { backgroundColor: colors.primary }]}>
          <Feather name="globe" size={21} color={colors.primaryForeground} />
        </View>
        <View>
          <Text style={[styles.brandTitle, { color: colors.foreground }]}>Pinduoduo</Text>
          <Text style={[styles.brandSubtitle, { color: colors.mutedForeground }]}>Переводчик экрана</Text>
        </View>
      </View>

      <View style={[styles.hero, { backgroundColor: colors.foreground }]}>
        <View style={styles.statusRow}>
          <View style={[styles.statusDot, { backgroundColor: colors.status }]} />
          <Text style={[styles.statusLabel, { color: colors.heroForeground }]}>ГОТОВ К ПЕРЕВОДУ</Text>
        </View>
        <Text style={[styles.heroTitle, { color: colors.heroForeground }]}>Китайский →{'\n'}русский</Text>
        <Text style={[styles.heroDescription, { color: colors.heroMuted }]}>
          Перевод текста прямо поверх Pinduoduo. Нажатия на оригинальные элементы остаются доступными.
        </Text>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Перевести экран"
          onPress={explainPreview}
          style={({ pressed }) => [
            styles.startButton,
            { backgroundColor: colors.primary, opacity: pressed ? 0.86 : 1 },
          ]}
        >
          <Feather name="maximize" size={18} color={colors.heroForeground} />
          <Text style={[styles.startButtonLabel, { color: colors.heroForeground }]}>Перевести</Text>
        </Pressable>
        {previewNotice ? (
          <Text style={[styles.previewNotice, { color: colors.heroMuted }]}>{previewNotice}</Text>
        ) : null}
      </View>

      <View style={[styles.privacyCard, { backgroundColor: colors.card }]}>
        <View style={styles.privacyTitleRow}>
          <Feather name="shield" size={17} color={colors.positive} />
          <Text style={[styles.privacyTitle, { color: colors.foreground }]}>
            Скриншоты не отправляются
          </Text>
        </View>
        <Text style={[styles.privacyDescription, { color: colors.mutedForeground }]}>
          OCR и перевод выполняются на устройстве. Интернет нужен только для первой загрузки языковой модели.
        </Text>
      </View>

      <View style={styles.sectionHeading}>
        <Text style={[styles.sectionTitle, { color: colors.mutedForeground }]}>
          ВНЕШНИЙ ВИД ПЕРЕВОДА
        </Text>
        <Text style={[styles.previewTag, { color: colors.mutedForeground }]}>
          предпросмотр
        </Text>
      </View>

      <View style={[styles.settingsCard, { backgroundColor: colors.card }]}>
        <View style={styles.settingHeader}>
          <Text style={[styles.settingLabel, { color: colors.foreground }]}>Размер текста</Text>
          <Text style={[styles.settingValue, { color: colors.mutedForeground }]}>{fontSize} sp</Text>
        </View>
        <View style={styles.sizeRow}>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Уменьшить размер текста"
            onPress={() => setFontSize((current) => Math.max(11, current - 1))}
            style={[styles.sizeButton, { backgroundColor: colors.secondary }]}
          >
            <Feather name="minus" size={17} color={colors.foreground} />
          </Pressable>
          <View style={[styles.sizeTrack, { backgroundColor: colors.secondary }]}>
            <View
              style={[
                styles.sizeProgress,
                { backgroundColor: colors.primary, width: `${((fontSize - 11) / 13) * 100}%` },
              ]}
            />
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Увеличить размер текста"
            onPress={() => setFontSize((current) => Math.min(24, current + 1))}
            style={[styles.sizeButton, { backgroundColor: colors.secondary }]}
          >
            <Feather name="plus" size={17} color={colors.foreground} />
          </Pressable>
        </View>
        <View style={styles.colorTitleRow}>
          <Feather name="droplet" size={16} color={colors.mutedForeground} />
          <Text style={[styles.settingLabel, { color: colors.foreground }]}>Цвет текста</Text>
        </View>
        <View style={styles.swatches}>
          {textColors.map((color) => (
            <Pressable
              key={color}
              accessibilityRole="button"
              accessibilityLabel={`Выбрать цвет ${color}`}
              onPress={() => setTextColor(color)}
              style={[
                styles.swatch,
                { backgroundColor: color, borderColor: textColor === color ? colors.foreground : colors.border },
              ]}
            >
              {textColor === color ? <View style={[styles.swatchSelected, { backgroundColor: colors.foreground }]} /> : null}
            </Pressable>
          ))}
        </View>
      </View>

      <View style={styles.footer}>
        <Feather name="info" size={14} color={colors.mutedForeground} />
        <Text style={[styles.footerText, { color: colors.mutedForeground }]}>
          В Android APK запрос разрешений появится только после нажатия «Перевести».
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { paddingHorizontal: 22, gap: 18 },
  brandRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 2 },
  brandMark: { width: 48, height: 48, borderRadius: 16, alignItems: 'center', justifyContent: 'center' },
  brandTitle: { fontSize: 23, fontWeight: '700', letterSpacing: -0.4 },
  brandSubtitle: { fontSize: 13, marginTop: 2 },
  hero: { borderRadius: 25, padding: 21, gap: 15 },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  statusDot: { width: 8, height: 8, borderRadius: 4 },
  statusLabel: { fontSize: 11, fontWeight: '700', letterSpacing: 1.1 },
  heroTitle: { fontSize: 31, lineHeight: 37, fontWeight: '600', letterSpacing: -0.6 },
  heroDescription: { fontSize: 13, lineHeight: 19 },
  startButton: { minHeight: 52, borderRadius: 15, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 10 },
  startButtonLabel: { fontSize: 15, fontWeight: '600' },
  previewNotice: { fontSize: 12, lineHeight: 17 },
  privacyCard: { padding: 19, borderRadius: 22, gap: 10 },
  privacyTitleRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  privacyTitle: { fontSize: 14, fontWeight: '600' },
  privacyDescription: { fontSize: 12, lineHeight: 18 },
  sectionHeading: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 2 },
  sectionTitle: { fontSize: 11, fontWeight: '700', letterSpacing: 1 },
  previewTag: { fontSize: 11 },
  settingsCard: { padding: 19, borderRadius: 22, gap: 15 },
  settingHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  settingLabel: { fontSize: 14, fontWeight: '500' },
  settingValue: { fontSize: 13 },
  sizeRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  sizeButton: { width: 34, height: 34, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  sizeTrack: { height: 6, borderRadius: 3, flex: 1, overflow: 'hidden' },
  sizeProgress: { height: '100%', borderRadius: 3 },
  colorTitleRow: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  swatches: { flexDirection: 'row', alignItems: 'center', gap: 13 },
  swatch: { width: 29, height: 29, borderRadius: 15, borderWidth: 2, alignItems: 'center', justifyContent: 'center' },
  swatchSelected: { width: 8, height: 8, borderRadius: 4 },
  footer: { flexDirection: 'row', alignItems: 'flex-start', gap: 9, paddingRight: 72, marginTop: -2 },
  footerText: { flex: 1, fontSize: 12, lineHeight: 17 },
});
