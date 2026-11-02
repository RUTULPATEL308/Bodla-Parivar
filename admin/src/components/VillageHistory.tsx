'use client';

import { useState } from 'react';
import { BookOpen, ExternalLink } from 'lucide-react';

type Language = 'gu' | 'en';

interface TableRow { cells: string[]; }
interface Section { title: string; body: string; table?: { headers: string[]; rows: TableRow[] }; }

const history: Record<Language, {
  eyebrow: string; title: string; subtitle: string; introduction: string;
  sections: Section[]; note: string; source: string;
}> = {
  gu: {
    eyebrow: 'ગામનો વારસો',
    title: 'બોદલા ગામનો ઇતિહાસ',
    subtitle: 'સ્મૃતિઓ, મૂળ અને પેઢીઓની સામૂહિક સફર',
    introduction: 'આજરોજ તા. ૨-૧૧-૨૦૦૮ને રવિવારે, સમસ્ત ચોર્યાસી કડવા પાટીદાર સમાજ - કેળવણી ઉત્તેજક મંડળ આયોજિત તેજસ્વી તારલા અભિવાદન કાર્યક્રમના યજમાન બનવાનું સદ્ભાગ્ય અમારા ગામને સાંપડ્યું છે. જેને સમસ્ત બોદલા ગ્રામ પરિવારનાં અહોભાગ્ય માનીએ છીએ. આપણી સંસ્થાના એક અંગ સમાન બોદલા ગામનો ઈતિહાસ અને તેની પ્રગતિની રૂપરેખા આપ સૌ સમક્ષ પુસ્તિકારૂપે પ્રગટ કરતાં અમે આનંદ અનુભવીએ છીએ. સમાજમાં અમારા અસ્તિત્વની જાણકારી ફેલાય, ઉપરાંત ખુદના ઉદ્ગમથી અજાણ એવા ગામ લોકોને પણ પોતાની સ્થિતિ અને પ્રગતિનો અહેસાસ થાય અને એમના ઉત્સાહમાં વધારો થાય એવી અપેક્ષાએ કરેલો આ પ્રયત્ન આપ સૌ અતિરેકમાં નહિં ખપાવશો એવી અભ્યર્થના.\n\nઆજથી ૪૦૦ વર્ષ પહેલાં ભારત જયારે દેશી રજવાડાંનું રાજય હતું ત્યારે રાજા પોતાની વસ્તીની માહિતી તેમણે નીમેલા ભાટ-ચારણો દ્વારા મેળવતા. દરેક ગામના અને વર્ણના પેઢીનામા લખવાના ઈજારા આપવામાં આવતા. ભાટ-ચારણો તેમને સોંપાયેલ કુટુંબોના પેઢીનામાના ચોપડા રાખતા. જેમાં વ્યક્તિ વિશેષના ઉલ્લેખ પણ સચવાતા. દર ચાર-પાંચ વર્ષે ગામે ગામ ફરી પેઢીમાં નવા જન્મનાર બાળકનો ઉમેરો નોંધવામાં આવતો. યજમાનો પોતાના બારોટને રહેવા-જમવાની વ્યવસ્થા સાચવતા અને ભેટ-સોગાદો પણ આપતા. આમ ગામના દરેક વ્યક્તિ પોતાની વંશાવલીથી માહિતગાર રહેતા. એ પ્રથા હાલમાં તો ભૂંસાઈ જવા આવી છે. પરંતુ એ પ્રથામાં ઉછરેલા આપણા દાદાઓ (૧) સ્વ. વેલાભા હરિદાસ (૨) સ્વ. ઈસ્વરભા શામળદાસ તથા (૩) માજી સરપંચ પુરુષોત્તમદાસ રામદાસ (૮૭) વિગેરેના મુખેથી અને બારોટના ચોપડે નોંધાયા પ્રમાણે બોદલા ગામનો ઈતિહાસ આપ સમક્ષ રજૂ કરતાં આનંદ અનુભવું છું.',
    sections: [
      {
        title: '(૧) સ્થાપના (Founding)',
        body: 'હાલના રહેઠાણવાળા ગામની સ્થાપના વિ.સં. ૧૯૧૦ યાને ઈ.સ. ૧૮૫૪માં થયેલી. જણાવેલ વડવાઓએ બાંધેલ છે.',
        table: {
          headers: ['વ્યક્તિ', 'હાલનો ટોડો', 'ક્યાંથી આવ્યા'],
          rows: [
            { cells: ['પટેલ જેસંગભા ઉમેદભા', 'વેલા-હરિ', 'હીંગળાજપુરા'] },
            { cells: ['પટેલ ઉમેદભા મૂળજીભા', 'રામા-ઉમેદ', 'કનોડા'] },
            { cells: ['પટેલ ધનજીભા હેમરાજભા', 'ઈશ્વર-શામળ', 'સદુથલા'] },
            { cells: ['પટેલ ધનજીભા વાલજીભા', 'વિરચંદ-ધનજી', 'કનોડા'] },
            { cells: ['પટેલ શીવરામ સવજીભા', 'માધા-શીવા', 'કુકરવાડા'] },
            { cells: ['ભગુભા ભવાનભા', 'જોઈતા-માધા', 'કુકરવાડા'] },
            { cells: ['પટેલ હરિભા બેચરભા', 'જોઈતા-શંકર', 'કનોડા'] },
          ],
        },
      },
      {
        title: '(૧) સ્થાપના - મૂળ અને વસવાટ',
        body: 'આપણા વડવાઓનું મૂળ વતન ઊંઝા અને શાખ "મોખાત" હતી. પરંતુ સમયાંતરે બારોટો તેમના કાર્યો ઉપરથી નવી શાખ પાડતા. ત્યાંથી અમુક કુટુંબો વિસનગર વસવા ગયેલા. ત્યાં તેમણે હવાડામાં ખાંડ નાખીને ગાયોને પાયેલી. ત્યારથી તેમની શાખ "ધોળુ" પડેલી. ત્યાં તેઓ ઘણા વર્ષ રહ્યા. તે સમયે લાલાભા નામના આપણા વડવા પોતાની બાડોશી અને દાનવીરતાથી બારોટોમાં ખૂબ નામકમાયેલા. પોતાનો વટ રાખવા તે ગમે તે કરી છૂટતા. આવો જ કોઈ વટ રાખવા તેમના વંશજો વિસનગરથી હિજરત કરી ગયેલા. ત્યારથી તેમની ઓળખ "લાલાવટ" ઉપરથી હાલમાં અપભ્રંશ લાલાવાત અથવા લાલવોત લખાય છે. ત્યાંથી ૩ કુટુંબો કનોડા, બે કુકરવાડા, એક સદુથલા, એક કુટુંબ હીંગળાજપુરા ગયેલું. જયારે ૨ કુટુંબો શોભાસણ (તા. વડનગર) મુકામે ગયેલાં.\n\nતે દરમ્યાન જૂના બોદલા ગામના અસલ વતની આહીરોએ ગામ ખાલી કરતાં, ઈટોડાથી આવીને વસેલા ચૌધરીઓ પણ દેલા મુકામે રહેવા જતા રહ્યા. તેમની જમીનો પડતર થતાં વિસનગરથી વિખરાયેલા આપણા દાદાઓએ પરસ્પર સંતલસ કરી, ગાયકવાડ સરકાર પાસેથી બોદલા તળીયાની પડતર જમીનોની માગણી કરી. જે મંજૂર થતાં ઉપર જણાવેલ વડવાઓએ બોદલામાં નવેસરથી વસવાટ કર્યો. આહીરો અને ચૌધરીઓએ ખાલી કરેલ જૂનું ગામ પીરભાના દેવળની આસપાસના વાડાઓમાં હતું. હાલમાં વિખૂટા પડેલા આપણા કુટુંબીઓ શોભાસણ (તા. વડનગર), સતલાસણા તથા બેચરપુરા-વિસનગરમાં વસે છે.\n\nશરૂમાં પાટીદારોનાં ૧૬ ઘર, ઠાકોરોનાં ૧૦ ઘર, ગોસ્વામી માધા ભારથીની મઢી, વાઘરીઓનાં ૨ ઘર (૧-બનામંગળા તથા ૨-રણછોડ ધના), હરિજનોમાં ૧-પશાધના તથા ૨-બેચર ધના, એ બે કુટુંબો પાછળથી આવીને વસેલાં. એક ઘર નરસિંહ સેનમાનું જેમોટપ સ્થળાંતર કરી ગયેલું. ઠાકોરોમાં મુખ્ય (૧) ગજાજી જેનો પાછલો વસ્તાર પુંજાજી તથા સવધાનજી, (૨) રત્નાજી (શિયોરા), (૩) વેલાજીના વંશમાં સૂરસંગજી તથા (૪) ઉમાજીના વંશમાં સૂરતાજી તથા દાંતીસણીયાના ૪ ઘરો હતા.',
        table: undefined,
      },
      {
        title: '(૨) જનસંખ્યા (Demographics)',
        body: 'એમ કુલ ૩૦ કુટુંબોથી વસેલું આ ગામ આજે ૧૬૦ ઘર પટેલો, ૫૦ ઘર ઠાકોરો, ૧૫ ઘર હરિજનો, ૧૦ ઘર પ્રજાપતિ, ૫ ઘર લુહાર, ૧ ઘર દરજી, ૧૦ ઘર રાવળ તથા ૬ ઘર વાઘરી એમ કુલ ૨૦૦ ઘરમાં આશરે ૧,૨૦૦ની વસ્તી ધરાવે છે. પાટીદારોનાં ૬૦થી ૬૫ કુટુંબો બહાર વસે છે.',
        table: {
          headers: ['સમુદાય', 'ઘર'],
          rows: [
            { cells: ['પાટીદાર (Patidar)', '૧૬૦'] },
            { cells: ['ઠાકોર (Thakor)', '૫૦'] },
            { cells: ['હરિજન (Harijan)', '૧૫'] },
            { cells: ['પ્રજાપતિ (Prajapati)', '૧૦'] },
            { cells: ['લુહાર (Luhar)', '૫'] },
            { cells: ['દરજી (Darji)', '૧'] },
            { cells: ['રાવળ (Ravul)', '૧૦'] },
            { cells: ['વાઘરી (Vaghari)', '૬'] },
            { cells: ['કુલ (Total)', '૨૦૦'] },
          ],
        },
      },
      {
        title: '(૩) આફતો (Calamities)',
        body: '(૧) કુદરતી: આજથી ૭૦ વર્ષ પહેલાં ગામને માથે કુદરતી પ્લેગ-મરકી ફાટી નિકળવાથી થયેલી. ૪૫ માણસોનાં મૃત્યુ નિપજેલાં. તે વખતે ગામના ભાણા તરીકે ઉપડવા ગામથી અહીં આવીને વસેલા પટેલ અમથા ભગતે પોતાની સાઘનાથી ખપ્પર લઈ રથ કાઢેલો. કહેવાય છે કે ત્યાર પછી આરોગ એકદમ અટકી ગયેલો.\n\n(૨) માનવીય: ગામ વસ્યા પછી રખેવાડી માટે ચોકી પ્રથા દાખલ કરેલી. પાછળથી ચોકી પ્રથા હોવા છતાં ચોરીઓનું પ્રમાણ વધેલું. લોકોએ ચોરોને વળતર ચૂકવવાં પડતાં. આથી કંટાળીને તે સમયના યુવાનોએ ચોકી પ્રથા નાબૂદ કરી, જાતે રખેવાડીનું બીડું ઝડપેલું. જેથી ચોકીયાતો અને પટેલો વચ્ચે મોટા ઝગડા ઉભા થયેલા. પરંતુ ગામનો સંપ જળવાઈ રહેતાં સમયાંતરે બધું શાંત પડેલું. દરેક સંકટ સમયે ગામ લોકોએ અભૂતપૂર્વ એકતા, સંપ જાળવી, તેમાં દરેક કોમના ભાઈઓએ સાથ અને સહકાર આપેલ છે.',
        table: undefined,
      },
      {
        title: '(૪) જીવન વ્યવસ્થા (Life & water)',
        body: 'પીવાના પાણી માટે ગામ લોકોએ જાતે ઊભી કરેલી. સમયાંતરે ગામ લોકોએ જાતે બનાવેલ ત્રણ કૂવામાં બે હાલમાં પડતર હયાત છે. ખેતરોમાં પીયત માટે પણ કૂવા ગળાવી, બળદોની મદદથી ચામડાના કોસ દ્વારા પાણી ખેંચવામાં આવતું અને શિયાળું પાકો પકવતા. મોટાભાગે પુરુષોનું ન્હાવા-ધોવાનું ગામ કૂવે રહેતું. જયારે સ્ત્રીઓ થોડીક આડ ઉભી કરીને ઘરઆંગણે નાડી લેતી. ખેતીની કમાણી ઉપર જ જીવન તતભતું. વિ.સં. ૧૯૨૦માં ડોસાશેઠ નામના વાણીયા ગામમાં આવીને વસેલા. ખેડૂતો તેમને ત્યાં પોતાની ઉપજ આપતા અને બદલામાં જીવન-જરૂરી માલ-સામાન તથા ધિરાણ મેળવતા.',
        table: undefined,
      },
      {
        title: '(૫) દેવસ્થાનો (Temples)',
        body: '(૧) દૂધેશ્વર મહાદેવ: આ ૪૦૦ વર્ષ પહેલાં આહીરોએ બંધાવ્યાનું કહેવાય છે. જેમાં ૧૫૦ વર્ષ પહેલાં આપણા વડવાઓએ કેલથી પ્લાસ્ટર કરાવેલ છે.\n\n(૨) ચામુંડા માતા: માતાજીની નાની દેરડીની પણ મહાદેવ સાથે જ સ્થાપના થયેલી. પરંતુ કોઈક કારણસર લાંબા સમય સુધી ગામમાં પાટીદારનું ૪૦ ઉપર ૪૧મું ઘર વસતાં જવિઘન આવતાં. તેથી તે સમયના કોઈ પ્રખ્યાત જ્યોતિષીને બોલાવી તેમના કહ્યા પ્રમાણે ફરીવાર વિધિપૂર્વક માતાજીની પ્રતિષ્ઠા કરી હાલનું મંદિર બંધાવેલ અને ત્યારથી અખંડ જ્યોત ચાલું છે.\n\n(૩) રામજી મંદિર: વિ.સં. ૧૯૨૫માં હાલના રામજી મંદિરનું બાંધકામ કરાવેલ છે. જેની પ્રતિષ્ઠા ગામલોકોના ગુરૂ રામચરણદાસજીની હાજરીમાં કુકરવાડાથી ૩૫ બ્રાહ્મણો બોલાવી કરેલ છે.\n\n(૪) દાસજીયા ગોગ: હાલના સુધારાને બાદ કરતાં ગોગા મહારાજનું મંદિર પણ એક સો વર્ષ પહેલાં બંધાયેલ છે.\n\n(૫) હિંદવા પીર: જૂના ગામઠણમાં અપૂજ બનેલ હિંદવા પીરની જગ્યા ઉપર લોકોએ વારંવાર પરચા થવાથી વેડા ગામથી મહારાજ બોલાવી જૂના સ્થાનકની માહિતી મેળવી ત્યાં વિધિસર સ્થાપના કરેલ છે.\n\n(૬) રામદેવજી પીર: ગામના ઉત્સાહી ઠાકોર ભાઈઓએ રામાપીરનું આખ્યાન ભજવવા મંડળ સ્થાપેલું અને ફાળો ઉભો કરેલ. ભાદરવા સુદ-૯મે મંદિર બંધાવેલ છે. નિભાવણી ખર્ચ ઠાકોરો સંભાળે છે. હિસાબ-વહીવટ પટેલોને સોંપેલો છે.\n\n(૭) મનોકામના સિદ્ધ હનુમાનજી: શ્રી રામદાસ મહારાજની પ્રેરણાથી બોદલા બસસ્ટેન્ડ પાસેની જમીન પર ગામ લોકોના સહકારથી આ તેમની ઈચ્છાથી મંદિર બાંધવા ગામ લોકોએ પણ સારો ફાળો આપી, સાથે મળી સને ૧૯૯૨-૯૩, મંદિર સને ૨૦૦૨માં ચૈત્ર સુદ-૧૫મે વિધિવત પ્રાણપ્રતિષ્ઠા કરેલ છે.',
        table: undefined,
      },
      {
        title: '(૬) અભ્યાસ (Education)',
        body: 'ગામ વસ્યા પછી વીસેક વર્ષ બાદ ગાયકવાડ સરકારે પ્રાથમિક શાળા શરૂ કરેલી. જેમાં મોટપ, કમાલપુર, હરદેસણ અને ઝીલોસણના વિદ્યાર્થીઓ ભણવા આવતા. બલોલના જટાશંકર ગાયકવાડી શિક્ષક મૂકાયેલા. આ વ્યવસ્થા વીસેક વર્ષ ચાલ્યા પછી શિક્ષક અને ગામ લોકોને અણબનાવ થવાથી શિક્ષકે શાળા બંધ કરાવેલ.\n\nસને ૧૯૫૧થી સ્વ. મણીલાલ રામદાસના પ્રયત્નોથી ગામ લોકોએ ગામઠી શાળા શરૂ કરાવેલ જેમાં દેવરાસણના નારણદાન ગઢવી શિક્ષક તરીકે સેવા આપતા.\n\nસને ૧૯૫૦માં ભારતમાં ગણતંત્ર રાજ શરૂ થતાં શિક્ષણને પ્રાથમિકતા મળી. સને ૧૯૫૪માં મહાદેવની જગ્યામાં પ્રથમ સરકારી પ્રાથમિક શાળા શરૂ થઈ. શિક્ષક તરીકે બોરીઆવીના માધવલાલ ચૌધરીની નિમણૂંક થઈ. તેમના સખત યોગદાનથી ગામમાં શિક્ષણનો પાયો નંખાયો અને આજ સુધી પાંગર્યો.',
        table: undefined,
      },
      {
        title: '(૭) પંચાયતી રાજ (Panchayat)',
        body: 'સને ૧૯૫૦ થી પંચાયતી રાજ સ્થપાયું ત્યારથી ગામમાં સુખાકારીનાં સાધનોની ઉપલબ્ધી થવા લાગી. ચૂંટાયેલા સરપંચો અને તેમની ઉપલબ્ધીઓ નીચે મુજબ છે.',
        table: {
          headers: ['સરપંચનું નામ', 'સમયગાળો'],
          rows: [
            { cells: ['મણીલાલ રામદાસ પટેલ', '૧૯૫૦-૧૯૫૫'] },
            { cells: ['જોઈતારામ શામળદાસ પટેલ', '૧૯૫૫-૧૯૬૫'] },
            { cells: ['પરસોત્તમ ઈશ્વરદાસ પટેલ', '૧૯૬૫-૧૯૭૦'] },
            { cells: ['પરસોત્તમ રામદાસ પટેલ', '૧૯૭૦-૧૯૮૦'] },
            { cells: ['રામાભાઈ શંકરદાસ પટેલ', '૧૯૮૦-૧૯૮૫'] },
            { cells: ['અમૃતભાઈ માધવદાસ પટેલ', '૧૯૮૫-૧૯૮૬'] },
            { cells: ['પરસોત્તમ જોઈતારામ પટેલ', '૧૯૮૬-૧૯૯૧'] },
            { cells: ['ભગાભાઈ શિવદાસ પટેલ', '૧૯૯૧-૧૯૯૬'] },
            { cells: ['કરશનજી કાનાજી ઠાકોર', '૧૯૯૬-૨૦૦૫'] },
            { cells: ['દિપાજી મફાજી ઠાકોર', '૨૦૦૫-૨૦૦૬'] },
            { cells: ['પ્રહલાદજી ખેંગારજી ઠાકોર', '૨૦૦૬ થી ચાલુ'] },
          ],
        },
      },
      {
        title: '(૭) પંચાયત - મુખ્ય ઉપલબ્ધીઓ',
        body: 'પ્રાથમિક શાળા, ગામ કૂવો, શાળાના બે રૂમ, તળાવનો વરંડો, ખરિયાવાટ, સેવા સહકારી મંડળી, એપ્રોચ રોડ, તળાવ ખોદકામ, ગામ કૂવો, શાળાના બે રૂમ, વોટર વર્કસ, જનતા ઘર, નોસોર્સ બોર, ઈલેક્ટ્રીકરણ, તળાવ ખોદકામ, તળાવનું રાહતકામ, રાહતી પતરાં તથા સંડાસ યોજના, રાહતકાર્યો, શાળાનો એક રૂમ, પાણી નળકનેક્શન, ગોકુળગામ યોજના, રોડ પીચીંગ, બાલમંદિર, બોર-૧, વરંડો, પંચાયતી બોર બીજો, આંગણવાડી, બસસ્ટેન્ડ, નર્મદાના મીઠા પાણીનું જોડાણ, ખરિયાવાટ, આશ્રમ રોડ માટીકામ.',
        table: undefined,
      },
      {
        title: '(૮) આવકનાં સાધનો (Income)',
        body: 'સને ૧૯૬૬-૬૭ સુધી ખેતી એકમાત્ર આવકનું સાધન હતું. ત્યારબાદ ભણેલા લોકો નોકરીએ જોડાયા. વેપારધંધા વધતાં કેટલાક વેપારમાં જોડાયા. હીરા ઉદ્યોગનો વિકાસ થતાં કેટલાક ભણતર છોડી હીરામાં જોડાયા. સને ૧૯૭૨ પછી ખેડૂતોએ પણ જમીન પીયત માટે જૂથો રચી પોતાના બોરીંગ બનાવી ખેતીની આવક વધારી. ડેરી ઉદ્યોગનો વિકાસ થતાં પટેલ અમૃતલાલ માધવદાસ તથા કાન્તિભાઈ રામદાસે મળી સૌ પ્રથમ ડેરી મંડળીની સ્થાપના કરી. સેવા સહકારી મંડળી સ્થપાઈ જેની લોન મેળવી લોકો ઢોર અને ખેતીનાં ઓજારો વસાવી કમાણી વધારવા લાગ્યા. કેટલાકે બોરીંગ રીપેરીંગ માટે રીંગ મશીનો વસાવી બોદલા ખાતે તે ધંધાની જિલ્લા કક્ષાએ નામના મેળવી. આમ ગામની આર્થિક પ્રગતિ કુદકે ને ભૂસકે વધવા લાગી.',
        table: undefined,
      },
      {
        title: '(૯) યુવક પ્રવૃત્તિઓ (Youth)',
        body: 'સને ૧૯૫૮થી શાળામાં ભણતા વિદ્યાર્થીઓએ યુવક મંડળની સ્થાપના કરી તેનું રજિસ્ટ્રેશન મેળવ્યું. યુવાનોએ તે સમયે જાત મહેનત કરી, એક બસસ્ટેન્ડ ઉપર તેમજ એક શાળાના કમ્પાઉન્ડમાં એમ બે પાકા કૂવા ગળાવી ત્યાં પાણીની વ્યવસ્થા ગોઠવી. શાળા તેમજ બસસ્ટેન્ડ ઉપર ૩૦ થી વધુ વૃક્ષો ઉછેર્યા. દર વર્ષે સાંસ્કૃતિક કાયક્રમો યોજી, શાળાના રૂમો, બાલ મંદિર તથા અન્ય સાર્વજનિક કામો માટે લોકફાળો ઉભો કરી પંચાયતોને મદદરૂપ બન્યા. સ્વ. પા. ઈશ્વરભાઈ ચેલદાસના પ્રયત્નોથી ગામમાં નવરાત્રીની ઉજવણી ચાલું થઈ. આથી સને ૧૯૮૫માં નવરાત્રી મંડળની સ્થાપના કરી.\n\nગામની આવક જાવકના તમામ પાંખના હિસાબોનો હવાલો સંભાળ્યો. દરેક દેવસ્થાનના નિભાવણી ખર્ચનું આયોજન કરી તેનો લેખિત વાર્ષિક અહેવાલ દર દશેરાએ ગામ સમક્ષ મૂકવાની પ્રથા સને ૧૯૮૮થી શરૂ કરવામાં આવી. જે આજે પણ ચાલું છે. આ આવકમાંથી મહાદેવની જગ્યાની ફરતે પાટીદાર વાડીનું નિર્માણ કર્યું. મંદિર સમારકામ કરાવ્યું. દેવસ્થાનોમાં કલરકામ કરાવ્યાં. શરૂમાં એકાદ-બે વર્ષે સાંસ્કૃતિક કાર્યક્રમો કરાતા. પરંતુ ૧૯૯૮ પછી ટી.વી.નો વ્યાપ વધતાં આ પ્રવૃત્તિ નહીવત બનેલ છે.',
        table: undefined,
      },
      {
        title: '(૧૦) ધાર્મિક પ્રવૃત્તિઓ (Religious)',
        body: '૧. સને ૧૯૮૫થી દર ગુરુવારે ગાયત્રી સમૂહ પ્રાર્થનાનું આયોજન આજદિન સુધી ચાલું છે.\n૨. છેલ્લાં ૨૦ વર્ષથી દર શનિવારે સ્વાધ્યાય પ્રવૃત્તિ ચાલું છે.\n૩. છેલ્લા ૧૫ વર્ષથી ચોટીલા પગપાળા સંઘનું આયોજન ચાલું છે.\n૪. મહિલા ભજન મંડળ દ્વારા દરરોજ રાત્રે આરતી પછી રામજી મંદિરમાં ભજન-ધૂન ચાલું છે.\n\nદર વર્ષે ચાર યજ્ઞ: (૧) નવચંડી - કારતક સુદ-૨, (૨) હનુમાનજી - ચૈત્રી સુદ-૧૫, (૩) ગોગા મહારાજ - નાગપંચમી, (૪) રામાપીર - ભાદરવા સુદ-૯. જેમાં પ્રથમ બે યજ્ઞોમાં સમૂહ જમણવાર થાય છે.',
        table: undefined,
      },
      {
        title: '(૧૧) વિકાસ ગાથા (Development)',
        body: 'છેલ્લા ૨૫ વર્ષોમાં થયેલ ગ્રામ વિકાસની ઝાંખી. ગામના પાટીદારોનાં કુલ ૧૯૦ કુટુંબની વસ્તીમાં ૧૦૦ કુટુંબો ગામમાં, જયારે ૬૦ કુટુંબો ધંધા અથવા નોકરી અર્થે બહાર વસે છે. માત્ર ખેતી ઉપર નભતાં કુટુંબ ૨૦. ખેતી અને નોકરીવાળા ૬૦. અભ્યાસ ક્ષેત્રે: ૩ ડૉક્ટર, ૪ M.Com, ૩ B.Com, ૨ B.Ed, ૨ M.A., ૪ LLB.\n\n૫૦ વર્ષ પહેલાં ગામમાં મોટા ભાગના ઘરો માટીના અને ઉપર નળીયાંવાળાં હતાં. આજે ૯૫ ટકા મકાનો પાકાં છે. ગામની ભૌતિક સંપત્તિનો સર્વે કરતાં હાલમાં ૧૩ ટ્રેક્ટર્સ, ૩૦ બોર રીપેરીંગ મશીનો, ૩૫ કારો, ૪ જીપ ગાડીઓ, પ માર્શલ/કોલીસ ગાડીઓ અને ૮ બોરીંગ (ટયુબવેલ) મોજૂદ છે.\n\nવિશેષતા: ગામના તમામ પાટીદારો એક જ બાપના વંશજ હોવાથી કુદરતી રીતે સંપ જળવાઈ રહે છે. આજ સુધીમાં ગામના સામૂહિક ફાળામાં કોઈએ ક્યારેય વિઘન ઉભું કર્યું નથી. જયાં જેટલો ફાળો જરૂરી હોય તેનાથી વધારે ફાળો હંમેશાં મળી રહેલ છે. છેલ્લા ૩૦ વર્ષનો આ અમારો અનુભવ છે. અને અમારું ગૌરવ પણ છે.',
        table: {
          headers: ['સાધન', 'સંખ્યા'],
          rows: [
            { cells: ['ટ્રેક્ટર્સ', '૧૩'] },
            { cells: ['બોર રીપેરીંગ મશીનો', '૩૦'] },
            { cells: ['કારો', '૩૫'] },
            { cells: ['જીપ ગાડીઓ', '૪'] },
            { cells: ['માર્શલ/કોલીસ ગાડીઓ', 'પ'] },
            { cells: ['બોરીંગ (ટયુબવેલ)', '૮'] },
          ],
        },
      },
      {
        title: '(૧૨) આભાર દર્શન (Acknowledgment)',
        body: 'સાદર, આપણા ઘરઆંગણે ગામદેવી/દેવતાઓનાં આશીર્વાદથી આજનો રૂડો અવસર, ગામલોકોનાં સંપૂર્ણ ભાવનાપૂર્ણ સાથ અને સહકારથી ઉજવવામાં આવે છે. તે બદલ આપણાં પાટીદાર ચોયસી સમાજનો ગૌરવ સાથે આભાર માનીએ છીએ.\n\nલી. બોદલા-ગામ ચોયસી કડવા પાટીદાર સમાજના "જયશ્રી કૃષ્ણ" "જય ઉમિયા માતા"',
        table: undefined,
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
        title: '(1) Founding (1854 CE)',
        body: "Bodla's present settlement was founded in VS 1910 (1854 CE). Seven Patel founders.",
        table: {
          headers: ['Person', 'Descendant Line', 'Origin'],
          rows: [
            { cells: ['Patel Jesangbhai Ummedbhai', 'Vela-Hari', 'Hinglajpura'] },
            { cells: ['Patel Ummedbhai Muljibhai', 'Rama-Ummed', 'Kanoda'] },
            { cells: ['Patel Dhanjibhai Hemrajbhai', 'Ishwar-Shamal', 'Saduthala'] },
            { cells: ['Patel Dhanjibhai Valjibhai', 'Virchand-Dhanji', 'Kanoda'] },
            { cells: ['Patel Shivram Savjibhai', 'Madha-Shiva', 'Kukarwada'] },
            { cells: ['Bhagubhai Bhavanbhai', 'Joita-Madha', 'Kukarwada'] },
            { cells: ['Patel Haribhai Becharbhai', 'Joita-Shankar', 'Kanoda'] },
          ],
        },
      },
      {
        title: '(1) Founding - Origins',
        body: "Ancestral home was Unjha with Mokhat branch. In Visnagar known as Lalavat. 3 families to Kanoda, 2 to Kukarwada, 1 to Saduthala, 1 to Hinglajpura, 2 to Shobhasan. When Ahirs and Chaudharys vacated, founders obtained wastelands from Gaekwad government. Initially 16 Patel, 10 Thakor, 2 Vaghari, 2 Harijan families.",
        table: undefined,
      },
      {
        title: '(2) Demographics',
        body: 'Started with 30 families; now 200 houses, ~1,200 people. 60-65 Patidar families live outside.',
        table: {
          headers: ['Community', 'Households'],
          rows: [
            { cells: ['Patidar (Patel)', '160'] },
            { cells: ['Thakor', '50'] },
            { cells: ['Harijan', '15'] },
            { cells: ['Prajapati', '10'] },
            { cells: ['Luhar', '5'] },
            { cells: ['Darji', '1'] },
            { cells: ['Ravul', '10'] },
            { cells: ['Vaghari', '6'] },
            { cells: ['Total', '200'] },
          ],
        },
      },
      {
        title: '(3) Calamities',
        body: '(1) Natural: 70 years ago plague killed 45. Amtha Bhagat stopped epidemic through rituals with a skull. (2) Human: thefts increased during watchman system; youth abolished it and took charge. During every crisis, village showed unprecedented unity across all communities.',
        table: undefined,
      },
      {
        title: '(4) Life & Water',
        body: 'Village arranged water supply. Three wells, two now defunct. Borewells for irrigation with leather buckets pulled by bullocks. Men bathed at village well; women at home. Life depended on farm income. 1920: Dosasheth (merchant) settled; farmers gave produce, received goods and loans.',
        table: undefined,
      },
      {
        title: '(5) Temples',
        body: '(1) Dudheshwar Mahadev: 400 years old (built by Ahirs); plastered 150 years ago. (2) Chamunda Mata: re-established; eternal flame. (3) Ramji Mandir: VS 1925; 35 Brahmins. (4) Dasjiya Gog: 100+ years. (5) Hindva Pir: re-established. (6) Ramdevji Pir: Thakors maintain. (7) Manokamna Siddh Hanumanji: consecrated 2002 (Chaitra Sud-15).',
        table: undefined,
      },
      {
        title: '(6) Education',
        body: 'Gaekwad government started primary school (students from Motap, Kamalpur, Hardesan, Jilosan). 1951: village school by Manilal Ramdas. 1954: first government primary school at Mahadev site (teacher: Madhavlal Chaudhary, Boriyavi). Today: 3 doctors, 4 M.Com, 3 B.Com, 2 B.Ed, 2 M.A., 4 LLB.',
        table: undefined,
      },
      {
        title: '(7) Panchayat',
        body: 'Panchayati Raj started 1950. Sarpanchs listed below. Achievements: primary school, village well, service cooperative, approach road, pond excavation, water works, electrification, Narmada water, bus stand, Anganwadi, Ashram road work.',
        table: {
          headers: ['Sarpanch', 'Term'],
          rows: [
            { cells: ['Manilal Ramdas Patel', '1950-1955'] },
            { cells: ['Joitaram Shamaldas Patel', '1955-1965'] },
            { cells: ['Parsottam Ishwardas Patel', '1965-1970'] },
            { cells: ['Parsottam Ramdas Patel', '1970-1980'] },
            { cells: ['Ramabhai Shankardas Patel', '1980-1985'] },
            { cells: ['Amrutbhai Madhavdas Patel', '1985-1986'] },
            { cells: ['Parsottam Joitaram Patel', '1986-1991'] },
            { cells: ['Bhagabhai Shivdas Patel', '1991-1996'] },
            { cells: ['Karshanji Kanaji Thakor', '1996-2005'] },
            { cells: ['Dipaji Mafaji Thakor', '2005-2006'] },
            { cells: ['Prahladji Khengarji Thakor', '2006-present'] },
          ],
        },
      },
      {
        title: '(7) Panchayat - Achievements',
        body: 'Primary school, village well, school rooms, pond veranda, K hariyavat, service cooperative, approach road, pond excavation, water works, Janata houses, nosors bore, electrification, relief works, school room, water connections, Gokulgam scheme, road pitching, Balmandir, bore-1, veranda, Panchayati bore, Anganwadi, bus stand, Narmada water connection, Ashram road work.',
        table: undefined,
      },
      {
        title: '(8) Income Sources',
        body: 'Until 1966-67 agriculture was sole income. Then jobs and business. Diamond industry. 1972+: borewells. Dairy: Amrutlal Madhavdas and Kantibhai Ramdas founded first dairy cooperative. Service cooperative. Bore repair machines gained district reputation.',
        table: undefined,
      },
      {
        title: '(9) Youth Activities',
        body: 'Youth Mandal established 1958. Wells and 30+ trees at bus stand and school. Annual cultural programs. Navratri Mandal (1985). Annual financial reports presented every Dussehra since 1988. Patidar Wadi built around Mahadev. Temple repairs. Cultural activities declined after 1998 due to TV.',
        table: undefined,
      },
      {
        title: '(10) Religious Activities',
        body: '1. Gayatri group prayer every Thursday (since 1985). 2. Swadhyay every Saturday (20 yrs). 3. Chotila foot pilgrimage (15 yrs). 4. Mahila Bhajan Mandal daily at Ramji Mandir. 4 annual Yagnas: Navchandi (Kartak Sud-2), Hanumanji (Chaitri Sud-15), Goga Maharaj (Nag Panchami), Ramapir (Bhadarva Sud-9). First two have community feast.',
        table: undefined,
      },
      {
        title: '(11) Development',
        body: 'Last 25 years of village development. 190 Patidar families: 100 in village, 60 outside. Farming only: 20, farming+jobs: 60. Education: 3 doctors, 4 M.Com, 3 B.Com, 2 B.Ed, 2 M.A., 4 LLB. 50 years ago mud houses; today 95% pucca. 13 tractors, 30 bore-repair machines, 35 cars, 4 jeeps, 5 marshal/colis, 8 tube wells. Unity: all Patidars descend from one ancestor; no one ever obstructed community donations.',
        table: {
          headers: ['Asset', 'Count'],
          rows: [
            { cells: ['Tractors', '13'] },
            { cells: ['Bore repair machines', '30'] },
            { cells: ['Cars', '35'] },
            { cells: ['Jeeps', '4'] },
            { cells: ['Marshal/Colis', '5'] },
            { cells: ['Tube wells', '8'] },
          ],
        },
      },
      {
        title: '(12) Acknowledgment',
        body: 'With respect, this occasion is celebrated with the blessings of village deities and the full emotional support and cooperation of village residents. We express our gratitude with pride to the Patidar Choryasi Samaj. - Bodla Village Choryasi Kadva Patidar Samaj. "Jay Shri Krishna" "Jay Umiya Mata"',
        table: undefined,
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
  sections: Section[];
  note: string;
  source: string;
}>;

export default function VillageHistory({ compact = false }: { compact?: boolean }) {
  const [language, setLanguage] = useState<Language>('gu');

  return (
    <section className={compact ? 'border-b border-brand-border py-6' : 'mx-auto max-w-5xl py-2'} style={{ color: '#1E293B' }}>
      <header className="flex flex-col gap-5 border-b pb-5 sm:flex-row sm:items-end sm:justify-between" style={{ borderColor: '#D7DEE8' }}>
        <div>
          <div className="flex items-center gap-2" style={{ color: '#D97706' }}>
            <BookOpen className="h-5 w-5" />
            <span className="text-xs font-semibold uppercase">{history[language].eyebrow}</span>
          </div>
          <h1 className="mt-2 text-2xl font-bold font-gujarati" style={{ color: '#1E293B' }}>{history[language].title}</h1>
          <p className="mt-1 text-sm" style={{ color: '#475569' }}>{history[language].subtitle}</p>
        </div>
        <div className="inline-flex self-start border sm:self-auto" role="group" aria-label="History language" style={{ borderColor: '#D7DEE8' }}>
          {(['gu', 'en'] as const).map((option) => (
            <button
              key={option}
              type="button"
              aria-pressed={language === option}
              onClick={() => setLanguage(option)}
              className="min-h-10 px-4 text-sm font-semibold transition-colors"
              style={language === option ? { backgroundColor: '#D97706', color: '#FFFFFF' } : { backgroundColor: '#F8FAFC', color: '#1E293B' }}
            >
              {option === 'gu' ? 'ગુજરાતી' : 'English'}
            </button>
          ))}
        </div>
      </header>

      <div className="py-5">
        <p className="max-w-3xl text-base leading-8 whitespace-pre-line" style={{ color: '#1E293B' }}>{history[language].introduction}</p>
      </div>

      <div className="border-y" style={{ borderColor: '#D7DEE8' }}>
        {history[language].sections.map((section, index) => (
          <article key={section.title} className="grid gap-2 py-5 sm:grid-cols-[3rem_minmax(0,1fr)] sm:gap-4">
            <span className="font-english text-sm font-semibold" style={{ color: '#D97706' }}>0{index + 1}</span>
            <div>
              <h2 className="text-lg font-bold font-gujarati" style={{ color: '#1E293B' }}>{section.title}</h2>
              <p className="mt-2 max-w-3xl text-sm leading-7 whitespace-pre-line" style={{ color: '#475569' }}>{section.body}</p>
              {section.table && (
                <div className="mt-4 overflow-x-auto">
                  <table className="min-w-full text-sm" style={{ borderCollapse: 'collapse' }}>
                    <thead>
                      <tr style={{ backgroundColor: '#F1EAE4' }}>
                        {section.table.headers.map((h) => (
                          <th key={h} className="px-3 py-2 text-left font-semibold" style={{ color: '#1E293B', border: '1px solid #D7DEE8' }}>{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {section.table.rows.map((row, ri) => (
                        <tr key={ri} style={{ backgroundColor: ri % 2 === 1 ? '#F8FAFC' : '#FFFFFF' }}>
                          {row.cells.map((cell, ci) => (
                            <td key={ci} className="px-3 py-2" style={{ color: '#475569', border: '1px solid #D7DEE8' }}>{cell}</td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </article>
        ))}
      </div>

      <footer className="flex flex-col gap-3 py-5 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-xs leading-6" style={{ color: '#475569' }}>{history[language].note}</p>
        <a
          href="/bodla-village-history-gu.pdf"
          target="_blank"
          rel="noreferrer"
          className="inline-flex min-h-10 items-center gap-2 self-start border px-3 text-sm font-semibold sm:self-auto"
          style={{ borderColor: '#D7DEE8', backgroundColor: '#F8FAFC', color: '#1E293B' }}
        >
          <ExternalLink className="h-4 w-4" />
          {history[language].source}
        </a>
      </footer>
    </section>
  );
}
