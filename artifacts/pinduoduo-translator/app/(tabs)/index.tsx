import { Feather } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useState, type ComponentProps } from 'react';
import {
  Linking,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useColors } from '@/hooks/useColors';

const MARKETPLACE_URL = 'https://mobile.yangkeduo.com/';

export default function BrowserHome() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const [linkStatus, setLinkStatus] = useState('');
  const topInset = Platform.OS === 'web' ? Math.max(insets.top, 67) : insets.top;
  const bottomInset = Platform.OS === 'web' ? Math.max(insets.bottom, 34) : insets.bottom;

  const openMarketplace = async () => {
    try {
      await Linking.openURL(MARKETPLACE_URL);
      setLinkStatus('Открывается мобильный сайт Pinduoduo.');
    } catch {
      setLinkStatus('Не удалось открыть сайт. Проверьте подключение к интернету.');
    }
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
          <Text style={[styles.brandTitle, { color: colors.foreground }]}>
            Pinduoduo
          </Text>
          <Text style={[styles.brandSubtitle, { color: colors.mutedForeground }]}>
            Русский браузер
          </Text>
        </View>
      </View>

      <View style={[styles.hero, { backgroundColor: colors.foreground }]}>
        <View style={styles.statusRow}>
          <View style={[styles.statusDot, { backgroundColor: colors.status }]} />
          <Text style={[styles.statusLabel, { color: colors.heroForeground }]}>
            НАТИВНЫЙ ANDROID-БРАУЗЕР
          </Text>
        </View>
        <Text style={[styles.heroTitle, { color: colors.heroForeground }]}>
          Покупки на Pinduoduo — по-русски
        </Text>
        <Text style={[styles.heroDescription, { color: colors.heroMuted }]}>
          Нативный APK открывает настоящий мобильный сайт внутри WebView и
          переводит китайский текст страницы на устройстве.
        </Text>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Открыть мобильный сайт Pinduoduo во внешнем браузере"
          testID="open-marketplace"
          onPress={openMarketplace}
          style={({ pressed }) => [
            styles.startButton,
            { backgroundColor: colors.primary, opacity: pressed ? 0.86 : 1 },
          ]}
        >
          <Feather name="external-link" size={18} color={colors.primaryForeground} />
          <Text style={[styles.startButtonLabel, { color: colors.primaryForeground }]}>
            Открыть сайт
          </Text>
        </Pressable>
        {linkStatus ? (
          <Text accessibilityLiveRegion="polite" style={[styles.linkStatus, { color: colors.heroMuted }]}>
            {linkStatus}
          </Text>
        ) : null}
      </View>

      <View style={[styles.previewNotice, { backgroundColor: colors.accent }]}>
        <Feather name="info" size={18} color={colors.accentForeground} />
        <Text style={[styles.previewNoticeText, { color: colors.accentForeground }]}>
          Эта кнопка открывает сайт вне Replit-превью. Встроенный WebView и
          перевод доступны в нативном Android APK.
        </Text>
      </View>

      <View style={[styles.card, { backgroundColor: colors.card }]}>
        <Text style={[styles.sectionTitle, { color: colors.foreground }]}>
          Как работает приложение
        </Text>
        <FeatureRow
          icon="globe"
          title="Настоящий сайт"
          description="Загрузка mobile.yangkeduo.com прямо в Android WebView."
          color={colors}
        />
        <FeatureRow
          icon="type"
          title="Перевод страницы"
          description="ML Kit переводит китайский DOM-текст на русский на устройстве."
          color={colors}
        />
        <FeatureRow
          icon="refresh-cw"
          title="Динамический контент"
          description="MutationObserver замечает новые элементы при прокрутке и загрузке."
          color={colors}
        />
        <FeatureRow
          icon="archive"
          title="Сохранение товаров"
          description="Изображения, ссылки, цены, числа и валюты остаются исходными."
          color={colors}
        />
      </View>

      <View style={[styles.privacyCard, { backgroundColor: colors.card }]}>
        <View style={styles.privacyTitleRow}>
          <Feather name="shield" size={17} color={colors.positive} />
          <Text style={[styles.privacyTitle, { color: colors.foreground }]}>
            Без OCR и захвата экрана
          </Text>
        </View>
        <Text style={[styles.privacyDescription, { color: colors.mutedForeground }]}>
          Приложение читает только текст DOM-страницы. Переводы сохраняются в
          локальном кэше; для первой загрузки языковой модели нужен интернет.
        </Text>
      </View>

      <Text style={[styles.footer, { color: colors.mutedForeground }]}>
        Доступность входа, оплаты и оформления заказов зависит от Pinduoduo,
        региона и установленных приложений. Их работу нельзя гарантировать.
      </Text>
    </ScrollView>
  );
}

type FeatureRowProps = {
  icon: ComponentProps<typeof Feather>['name'];
  title: string;
  description: string;
  color: ReturnType<typeof useColors>;
};

function FeatureRow({ icon, title, description, color }: FeatureRowProps) {
  return (
    <View style={styles.featureRow}>
      <View style={[styles.featureIcon, { backgroundColor: color.secondary }]}>
        <Feather name={icon} size={17} color={color.primary} />
      </View>
      <View style={styles.featureCopy}>
        <Text style={[styles.featureTitle, { color: color.foreground }]}>{title}</Text>
        <Text style={[styles.featureDescription, { color: color.mutedForeground }]}>
          {description}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { paddingHorizontal: 22, gap: 16 },
  brandRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 2 },
  brandMark: { width: 48, height: 48, borderRadius: 16, alignItems: 'center', justifyContent: 'center' },
  brandTitle: { fontSize: 22, fontWeight: '700', letterSpacing: -0.4 },
  brandSubtitle: { fontSize: 13, marginTop: 2 },
  hero: { borderRadius: 25, padding: 21, gap: 15 },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  statusDot: { width: 8, height: 8, borderRadius: 4 },
  statusLabel: { fontSize: 10, fontWeight: '700', letterSpacing: 0.7 },
  heroTitle: { fontSize: 29, lineHeight: 35, fontWeight: '600', letterSpacing: -0.5 },
  heroDescription: { fontSize: 14, lineHeight: 21 },
  startButton: { minHeight: 50, borderRadius: 15, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 10 },
  startButtonLabel: { fontSize: 15, fontWeight: '600' },
  linkStatus: { fontSize: 12, lineHeight: 17 },
  previewNotice: { flexDirection: 'row', alignItems: 'flex-start', gap: 10, padding: 15, borderRadius: 16 },
  previewNoticeText: { flex: 1, fontSize: 12, lineHeight: 18 },
  card: { padding: 18, borderRadius: 20, gap: 17 },
  sectionTitle: { fontSize: 16, fontWeight: '700' },
  featureRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 12 },
  featureIcon: { width: 34, height: 34, borderRadius: 11, alignItems: 'center', justifyContent: 'center' },
  featureCopy: { flex: 1, gap: 3, paddingTop: 1 },
  featureTitle: { fontSize: 13, fontWeight: '600' },
  featureDescription: { fontSize: 12, lineHeight: 17 },
  privacyCard: { padding: 18, borderRadius: 20, gap: 9 },
  privacyTitleRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  privacyTitle: { fontSize: 14, fontWeight: '600' },
  privacyDescription: { fontSize: 12, lineHeight: 18 },
  footer: { fontSize: 11, lineHeight: 17, paddingHorizontal: 2 },
});
