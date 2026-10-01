# LGD-010 Music Provenance Inventory

Inventory date: 2026-08-28

This record reports the metadata actually embedded in the Principal-supplied files. It is not an independent legal opinion. Hashes and source-to-package mappings are preserved separately in `LGD-007_AUDIO_IMPORT.md`.

## Shared embedded metadata

Every `muss_01.mp3` through `muss_15.mp3` file identifies:

- composer/artist: Modest Mussorgsky;
- album: *Mussorgsky's Pictures at an Exhibition*;
- performer, in the comment field: The Skidmore College Orchestra;
- source URL, in the comment field: `https://www.classicals.de`;
- copyright tag: `Public Domain: http://creativecommons.org/licenses/publicdomain/`.

No separate license file or acquisition receipt was present in the workspace. LGD-012 independently located the matching current source page at `https://www.classicals.de/mussorgsky-exhibition`. It lists the same 15 movements, identifies The Skidmore College Orchestra and Musopen as the performer/source, and marks this specific recording set `CC PDM 1.0` (Creative Commons Public Domain Mark 1.0 Universal). The page requests credit to `www.classicals.de`.

## Recording inventory

| Source/package filename | Embedded title |
|---|---|
| `muss_01.mp3` | Promenade — Allegro giusto nel modo russico… |
| `muss_02.mp3` | I. Gnomus — Vivo |
| `muss_03.mp3` | Promenade — Moderato comodo e con delicatezza |
| `muss_04.mp3` | III. Tuileries — Allegretto non troppo capriccioso |
| `muss_05.mp3` | IV. Bydlo — Sempre moderato pesante |
| `muss_06.mp3` | Promenade — Tranquillo |
| `muss_07.mp3` | V. Ballet des poussins dans leurs coques |
| `muss_08.mp3` | VI. Samuel Goldenberg and Schmuyle — Andante |
| `muss_09.mp3` | X. La grande porte de Kiev — Allegro alla breve |
| `muss_10.mp3` | IX. La Cabane sur des pattes de poule |
| `muss_11.mp3` | Cum mortuis in lingua mortua |
| `muss_12.mp3` | VIII. Catacombae. Sepulcrum romanum |
| `muss_13.mp3` | VII. Limoges. Le Marché |
| `muss_14.mp3` | Promenade — Moderato non tanto pesante |
| `muss_15.mp3` | II. Il vecchio castello — Andante |

The source and packaged MP3 filenames are identical. The packaged files are byte-for-byte copies of the intake files.

## Selection sound

`temp/click.mp3` is a 0.078-second Principal-supplied selection effect with no embedded descriptive, author, source, copyright, or license tags. Android playback uses the losslessly decoded derivative `app/src/main/res/raw/ui_click.wav`; provenance beyond Principal supply remains undocumented and must be resolved before public distribution.

## LGD-012 release gate

- Music: **B — attribution required by project release policy.** Affirmative embedded metadata and a matching current source page identify the exact 15-piece set as CC Public Domain Mark 1.0. Credits name the composition, performer and Classicals.de. PDM is a status label rather than a legal dedication, and the source page disclaims warranties outside the EU, so this is evidence—not a legal opinion.
- Click: **C — provenance unclear.** No embedded provenance, source record or license exists. Public redistribution remains blocked pending Principal clearance or replacement under a later authorized work order. The asset remains acceptable for a private/local technical release candidate.

Creative Commons explains that PDM labels a work believed free of known copyright restrictions but is not legally operative in the way CC0 is. See `https://creativecommons.org/public-domain/pdm/` and `https://wiki.creativecommons.org/wiki/CC0_FAQ`.
