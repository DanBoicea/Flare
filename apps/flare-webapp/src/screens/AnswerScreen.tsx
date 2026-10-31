import type { Answer } from '../api/types'
import { useI18n } from '../i18n/context'

interface Props {
  answer: Answer
  onHelped: () => void
  onSendToStaff: () => void
}

/** Placeholder from W01; W03 finalises it. The answer is shown as plain text only. */
export function AnswerScreen({ answer, onHelped, onSendToStaff }: Props) {
  const { t } = useI18n()
  return (
    <div className="screen">
      <p>{t('answer.label')}</p>
      <h1 className="screen__title">{answer.found ? answer.text : t('answer.notFound')}</h1>
      <button type="button" className="button button--primary" onClick={answer.found ? onHelped : onSendToStaff}>
        {answer.found ? t('answer.helped') : t('answer.sendToStaff')}
      </button>
      <button type="button" className="button" onClick={answer.found ? onSendToStaff : onHelped}>
        {answer.found ? t('answer.sendToStaff') : t('answer.helped')}
      </button>
    </div>
  )
}
