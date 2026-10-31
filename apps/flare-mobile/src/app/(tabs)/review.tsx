import { Placeholder } from '@/components/Placeholder';
import { useI18n } from '@/i18n/context';

export default function ReviewScreen() {
  const { t } = useI18n();
  return <Placeholder message={t('review.placeholder')} />;
}
