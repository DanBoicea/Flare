import type { Answer } from '../api/types'
import { useI18n } from '../i18n/context'
import { Quote } from '../components/Quote'

interface Props {
  question: string
  answer: Answer
  onHelped: () => void
  onSendToStaff: () => void
}

/**
 * Answer (design 03). The answer is rendered as plain text (React escapes it; line breaks kept by CSS),
 * never as HTML, because it comes from an LLM. It is labelled as automatic and as possibly wrong.
 */
export function AnswerScreen({ question, answer, onHelped, onSendToStaff }: Props) {
  const { t } = useI18n()
  const found = answer.found

  return (
    <div className="screen">
      <Quote label={t('answer.youAsked')} text={question} />

      <section className="answer" aria-labelledby="answer-label">
        <h1 id="answer-label" className="answer__label">
          {t('answer.label')}
        </h1>
        {found ? (
          <>
            <p className="answer__text" lang="">
              {answer.text}
            </p>
            <p className="answer__disclaimer">{t('answer.disclaimer')}</p>
          </>
        ) : (
          <p className="answer__text">{t('answer.notFound')}</p>
        )}
      </section>

      {/* When nothing was found, asking staff becomes the main action. */}
      <div className="screen__actions">
        {found ? (
          <>
            <button type="button" className="button button--primary" onClick={onHelped}>
              {t('answer.helped')}
            </button>
            <button type="button" className="button" onClick={onSendToStaff}>
              {t('answer.sendToStaff')}
            </button>
          </>
        ) : (
          <>
            <button type="button" className="button button--primary" onClick={onSendToStaff}>
              {t('answer.sendToStaff')}
            </button>
            <button type="button" className="button" onClick={onHelped}>
              {t('confirm.another')}
            </button>
          </>
        )}
      </div>
    </div>
  )
}
