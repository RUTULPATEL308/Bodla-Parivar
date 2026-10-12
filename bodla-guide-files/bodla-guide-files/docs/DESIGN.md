# Design System

> DESIGN.md = HOW IT SHOULD LOOK AND FEEL. Source: project README.

## Style
Warm, respectful, village-friendly, simple. Gujarati is first-class, not an afterthought.

## Typography
- Gujarati: **Anek Gujarati**, body text 16sp or larger
- English and numerals: **DM Sans**

## Colors
| Token | Hex |
|---|---|
| Primary Saffron | `#D97706` |
| Secondary Leaf Green | `#4F7D4A` |
| Accent Golden | `#E9A23B` |
| Background Cream | `#FFF8E7` |
| Surface Warm White | `#FFFCF5` |
| Text Primary | `#292524` |
| Text Secondary | `#78716C` |
| Border | `#E7E5E4` |
| Icon Container | `#FEF3C7` |

## Components
- Icon container: 48dp x 48dp, 14dp radius, icon in `#D97706`
- Icons: Material Symbols Rounded (outline)
- Buttons: Primary (saffron), Secondary, Destructive
- Cards: Surface Warm White with Border color

## UX Requirements
- Bilingual labels (Gujarati + English), Gujarati first
- Loading, empty and error states on every list/screen
- Safe, localized error messages (never raw DB errors)
- Mobile responsive admin console; accessible forms
- Works offline on mobile with a visible sync status
