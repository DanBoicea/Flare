import { PhoneIcon } from '../components/PhoneIcon'
import { useI18n } from '../i18n/context'
import { translate } from '../i18n/strings'

/**
 * Emergency (design 06). Fixed text only: no medical or security advice.
 * The title is shown in both languages whatever the UI language, because someone in a hurry
 * will not look for the language button.
 */
export function EmergencyScreen({ onBack }: { onBack: () => void }) {
  const { language, t } = useI18n()
  const other = language === 'ro' ? 'en' : 'ro'

  return (
    <div className="screen">
      <section className="emergency" role="alert">
        <h1 className="emergency__title">{t('emergency.title')}</h1>
        <p className="emergency__other" lang={other}>
          {translate(other, 'emergency.title')}
        </p>
        <a className="emergency__call" href="tel:112">
          <PhoneIcon size={28} />
          {t('emergency.call')}
        </a>
      </section>
      <p className="emergency__body">{t('emergency.body')}</p>
      <button type="button" className="button" onClick={onBack}>
        {t('emergency.back')}
      </button>
    </div>
  )
}
