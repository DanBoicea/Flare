import { Placeholder } from '@/components/Placeholder';
import { useI18n } from '@/i18n/context';

export default function IssueDetailScreen() {
  const { t } = useI18n();
  return <Placeholder message={t('detail.placeholder')} />;
}
