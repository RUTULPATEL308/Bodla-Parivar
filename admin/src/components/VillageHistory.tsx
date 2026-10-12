'use client';

import { useState } from 'react';
import { BookOpen, ExternalLink } from 'lucide-react';

type Language = 'gu' | 'en';

const history = {
  gu: {
    eyebrow: 'ગામનો વારસો',
    title: 'બોદલા ગામનો ઇતિહાસ',
    subtitle: 'સ્મૃતિઓ, મૂળ અને પેઢીઓની સામૂહિક સફર',
    introduction: 'આ પુસ્તિકા બોદલાની કહાનીને પરિવારોનાં મૂળ અને ગામના સામૂહિક જીવન સાથે જોડે છે. તેમાં કુટુંબો, ધાર્મિક સ્થળો, શિક્ષણ, ગ્રામ પંચાયત અને ગામની પ્રગતિ જેવા વિષયો દ્વારા સમય સાથે બદલાતા જીવનની ઝલક મળે છે. નીચેનો પરિચય આ વિષયોને વાંચવા યોગ્ય લાંબા વર્ણનમાં ગોઠવે છે. ચોક્કસ નામો, તારીખો અને આંકડાઓ માટે મૂળ ગુજરાતી પુસ્તિકા જુઓ.',
    sections: [
      {
        title: 'વસવાટ અને પરિવારનાં મૂળ',
        body: 'બોદલા ગામનો ઇતિહાસ અહીં વસેલા પરિવારોની સ્મૃતિઓ અને તેમના મૂળ સાથે ગૂંથાયેલો છે. ઇતિહાસ પુસ્તિકા કુટુંબોનાં નામો, તેમની વંશપરંપરા અને તેઓ કયા સ્થળોથી આવ્યા તેની વિગતો નોંધે છે. આ વિગતો માત્ર અલગ અલગ પરિવારોની ઓળખ જાળવતી નથી, પરંતુ સમય સાથે ગામમાં બનેલા સંબંધો અને સહજીવનની કડી પણ દર્શાવે છે. પેઢી દર પેઢી પરિવારોની મહેનત, પરંપરા અને ગામ સાથેનું જોડાણ બોદલાની સામૂહિક ઓળખનો પાયો બન્યું છે. પુસ્તિકામાં નોંધાયેલાં કુટુંબવૃત્તાંત ગામના ભૂતકાળને લોકોની યાદો અને તેમના મૂળ સાથે જોડે છે.',
      },
      {
        title: 'ધાર્મિક અને સામૂહિક સ્થળો',
        body: 'બોદલાનાં મંદિરો અને પવિત્ર સ્થળો ગામની ધાર્મિક શ્રદ્ધા સાથે સાથે લોકોના સામૂહિક જીવનની ઓળખ પણ છે. આવા સ્થળોએ પૂજા, પરંપરા અને પ્રસંગો દ્વારા ગ્રામજનોને એકસાથે જોડ્યા છે. ઇતિહાસ પુસ્તિકા આ સ્થળોને યાદ કરતાં તેમની સાથે જોડાયેલી સામાજિક સ્મૃતિ અને ગામના જીવનમાં તેમના સ્થાનને પણ ઉજાગર કરે છે. ગામનો વારસો માત્ર જૂની ઇમારતોમાં જ નહીં, પરંતુ લોકો જાળવી રાખે છે તેવી રીત-રિવાજો, પરસ્પર સહકાર અને પેઢીઓથી વહેંચાતી યાદોમાં પણ જીવંત રહે છે.',
      },
      {
        title: 'શિક્ષણ અને પંચાયત',
        body: 'ગામની પ્રાથમિક શાળા અને શિક્ષણની વધતી તકો બોદલાના સામાજિક પરિવર્તનની મહત્વની કડી છે. શાળાએ બાળકોને ભણવાની જગ્યા આપી અને આગળ વધવાની નવી શક્યતાઓ ખોલી; શિક્ષણ પ્રત્યેનો પ્રયાસ સમય સાથે અનેક પરિવારોના જીવનનો ભાગ બન્યો. પુસ્તિકા ગ્રામ પંચાયતની ભૂમિકા અને ગામના જાહેર જીવનમાં થયેલા પ્રયાસોને પણ નોંધે છે. સ્થાનિક જરૂરિયાતો ઓળખવી, ગામલોકો સાથે મળીને નિર્ણય લેવો અને જાહેર સુવિધાઓ માટે પ્રયત્ન કરવો એ ગામના વિકાસમાં સહભાગિતાનું મહત્વ દર્શાવે છે.',
      },
      {
        title: 'ગામનો બદલાતો સમય',
        body: 'ખેતી બોદલાના જીવન અને પરિવારોની આજીવિકાનો મહત્વનો આધાર રહી છે. તેની સાથે ગામની જાહેર સુવિધાઓ, સામાજિક સંસ્થાઓ અને યુવાનોની પ્રવૃત્તિઓએ બદલાતી જરૂરિયાતોનો જવાબ આપ્યો છે. ઇતિહાસ પુસ્તિકા આ જુદા જુદા પાસાઓને ગામની આગળ વધતી સફર તરીકે રજૂ કરે છે: જ્યાં રોજિંદા જીવનની મહેનત, સામૂહિક પહેલ અને એકબીજાને આપેલો સહકાર સાથે મળીને પ્રગતિ લાવે છે. આ વારસો કોઈ એક વ્યક્તિ કે સમયગાળાની કહાની નથી; તે અનેક પેઢીઓએ પોતાના ગામ માટે કરેલા યોગદાનની સંયુક્ત સ્મૃતિ છે.',
      },
    ],
    note: 'ચોક્કસ નામો, તારીખો અને આંકડાઓ માટે મૂળ ગુજરાતી પુસ્તિકા જુઓ.',
    source: 'મૂળ ગુજરાતી પુસ્તિકા જુઓ',
  },
  en: {
    eyebrow: 'Village heritage',
    title: 'History of Bodla Village',
    subtitle: 'Memories, roots and a story shared across generations',
    introduction: "The booklet connects Bodla's story with the roots of its families and the shared life of the village. Through its themes of families, places of worship, education, the Gram Panchayat and village development, it offers a glimpse of how life has changed over time. The account below brings these themes together in a longer narrative. Refer to the original Gujarati booklet for names, dates and figures.",
    sections: [
      {
        title: 'Settlement and family roots',
        body: "The history of Bodla is woven together with the memories and roots of the families who made their home here. The history booklet records family names, lineages and the places from which families came. These details preserve not only the identity of individual families, but also the ties and shared life that grew among them over time. Across generations, families' work, traditions and connection to the village have helped shape Bodla's common identity. The family histories recorded in the booklet connect the village's past with the memories and origins of its people.",
      },
      {
        title: 'Places of worship and community',
        body: "Bodla's temples and sacred places express both religious faith and the shared life of the village. Through worship, traditions and community occasions, these places have brought residents together. In remembering them, the history booklet also highlights their place in village life and the social memories connected with them. A village's heritage lives not only in old buildings, but also in the customs people preserve, the support they give one another and the memories passed between generations.",
      },
      {
        title: 'Education and village governance',
        body: "The village primary school and expanding access to education are an important part of Bodla's social change. The school gave children a place to learn and opened new possibilities for the future; over time, the pursuit of education became part of life for many families. The booklet also records the role of the Gram Panchayat and efforts in public life. Identifying local needs, making decisions with residents and working for shared facilities show how participation has contributed to the village's development.",
      },
      {
        title: 'A village growing through time',
        body: "Agriculture has long been an important foundation of life and livelihood for Bodla's families. Alongside it, public facilities, community organizations and youth activities have responded to the village's changing needs. The history booklet brings these different parts together as a continuing story of a village moving forward, where daily work, shared initiatives and mutual support all contribute to progress. This heritage is not the story of one person or one period; it is the shared memory of what generations of residents have contributed to their village.",
      },
    ],
    note: 'Refer to the original Gujarati booklet for names, dates and figures.',
    source: 'View the original Gujarati booklet',
  },
} satisfies Record<Language, {
  eyebrow: string;
  title: string;
  subtitle: string;
  introduction: string;
  sections: { title: string; body: string }[];
  note: string;
  source: string;
}>;

export default function VillageHistory({ compact = false }: { compact?: boolean }) {
  const [language, setLanguage] = useState<Language>('gu');
  const content = history[language];

  return (
    <section className={compact ? 'border-b border-brand-border py-6' : 'mx-auto max-w-5xl py-2'}>
      <header className="flex flex-col gap-5 border-b border-brand-border pb-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="flex items-center gap-2 text-saffron">
            <BookOpen className="h-5 w-5" />
            <span className="text-xs font-semibold uppercase">{content.eyebrow}</span>
          </div>
          <h1 className="mt-2 text-2xl font-bold text-brand-text font-gujarati">{content.title}</h1>
          <p className="mt-1 text-sm text-brand-muted">{content.subtitle}</p>
        </div>
        <div className="inline-flex self-start border border-brand-border sm:self-auto" role="group" aria-label="History language">
          {(['gu', 'en'] as const).map((option) => (
            <button
              key={option}
              type="button"
              aria-pressed={language === option}
              onClick={() => setLanguage(option)}
              className={`min-h-10 px-4 text-sm font-semibold transition-colors ${language === option ? 'bg-saffron text-white' : 'bg-cream-surface text-brand-text hover:bg-saffron-container'}`}
            >
              {option === 'gu' ? 'ગુજરાતી' : 'English'}
            </button>
          ))}
        </div>
      </header>

      <div className="py-5">
        <p className="max-w-3xl text-base leading-8 text-brand-text">{content.introduction}</p>
      </div>

      <div className="divide-y divide-brand-border border-y border-brand-border">
        {content.sections.map((section, index) => (
          <article key={section.title} className="grid gap-2 py-5 sm:grid-cols-[3rem_minmax(0,1fr)] sm:gap-4">
            <span className="font-english text-sm font-semibold text-saffron">0{index + 1}</span>
            <div>
              <h2 className="text-lg font-bold text-brand-text font-gujarati">{section.title}</h2>
              <p className="mt-2 max-w-3xl text-sm leading-7 text-brand-muted">{section.body}</p>
            </div>
          </article>
        ))}
      </div>

      <footer className="flex flex-col gap-3 py-5 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-xs leading-6 text-brand-muted">{content.note}</p>
        <a
          href="/bodla-village-history-gu.pdf"
          target="_blank"
          rel="noreferrer"
          className="inline-flex min-h-10 items-center gap-2 self-start border border-brand-border bg-cream-surface px-3 text-sm font-semibold text-brand-text hover:bg-saffron-container sm:self-auto"
        >
          <ExternalLink className="h-4 w-4" />
          {content.source}
        </a>
      </footer>
    </section>
  );
}