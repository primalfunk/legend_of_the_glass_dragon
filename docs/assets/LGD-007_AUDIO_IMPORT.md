# LGD-007 audio import record

Date: 2026-08-28

The Principal-supplied files remain unchanged in `temp/`. Runtime code has no dependency on that intake directory.

## Music mapping

All music files were copied byte-for-byte into `app/src/main/res/raw/`. The source names already satisfy Android resource naming rules.

| Principal source | Packaged resource | Duration | SHA-256 |
|---|---|---:|---|
| `temp/muss_01.mp3` | `raw/muss_01.mp3` | 101.640 s | `B907A9D665BE5841AD2E5448E02A264FF5821FC33B6D17AD9450B0F255852263` |
| `temp/muss_02.mp3` | `raw/muss_02.mp3` | 163.656 s | `BA64FE20B48269AC5FBDD1493105E748D229C35D0F9B9DA58DDD875AF94400E2` |
| `temp/muss_03.mp3` | `raw/muss_03.mp3` | 58.200 s | `F0A0443BD8379E27E416289EA30773E8E4494BE62F7D82EAFB4EF1B563687B40` |
| `temp/muss_04.mp3` | `raw/muss_04.mp3` | 63.672 s | `C45F15596BD5F8BDC561413497F2F768A0849A8662ECDE7D22BDD6AB89AA7E1A` |
| `temp/muss_05.mp3` | `raw/muss_05.mp3` | 140.664 s | `07E85F1ED2D1B05CB9A65729AF899EA055A76F6BD1CD758544709C06DA5BB464` |
| `temp/muss_06.mp3` | `raw/muss_06.mp3` | 42.768 s | `98CCB6281A30558F13D37A68C4DB14E234F7326532C97B1D7677183BE7C81C1B` |
| `temp/muss_07.mp3` | `raw/muss_07.mp3` | 80.064 s | `5C904F2BBBCDD9CA43A4E3CD5D91AA1CDBACCCBE9A3B7BF37CB8806B3D8DAA51` |
| `temp/muss_08.mp3` | `raw/muss_08.mp3` | 130.248 s | `CF4A39D5AC1286E4A6CF6C6F69BD90E44CF867CB722A5CB4673CECE9FEC6734C` |
| `temp/muss_09.mp3` | `raw/muss_09.mp3` | 329.424 s | `ABBE3AF0F6086E926452469DE418134F81C91D2C74C0921B1231134D6C8A4422` |
| `temp/muss_10.mp3` | `raw/muss_10.mp3` | 212.856 s | `3C922071CAD44D957A79A478D424130F55722AAB7E588C44213EF3016915E810` |
| `temp/muss_11.mp3` | `raw/muss_11.mp3` | 112.608 s | `A7D7BDD7E1D3E38E17E5B26B0FC38A56FD86EB1FEAEEE34F0A028DBBCE1BB058` |
| `temp/muss_12.mp3` | `raw/muss_12.mp3` | 113.232 s | `75B46BE31437B9A175B02FAB31C8AB9C81DA26C9B9AB58DA34CDCCF4C2B3330D` |
| `temp/muss_13.mp3` | `raw/muss_13.mp3` | 100.176 s | `BB5DD4D0AC9BC5DC7DA7A7258249A35A951AEE5534F4E29CD98DC54AF4767BF2` |
| `temp/muss_14.mp3` | `raw/muss_14.mp3` | 31.968 s | `820707329D1A6014E400A6E65A281D7CCE9D8C58FCA3D2CCDBDEEF11B43F4E57` |
| `temp/muss_15.mp3` | `raw/muss_15.mp3` | 286.608 s | `970F2F43187B7DC4FA215FBAFB9E5EDC3EB6C78B14E52AD25DD63A47B5F940CB` |

Source and packaged hashes were compared after import; all 15 pairs match.

## Selection sound mapping

| Principal source | Production resources | Disposition |
|---|---|---|
| `temp/click.mp3` | `raw/ui_click.wav` | The original 1,956-byte MP3 remains untouched in the intake directory (`727E629C2657802491CFAA05FECC830FA47BA9C804D3BC1E91DC87C4A357935F`) and is not redundantly packaged. The TCL NXTPAPER's SoundPool and MediaPlayer both rejected this unusually short MP3 and logged `unable to load sound`. A 44.1 kHz mono PCM16 WAV derivative was therefore decoded with FFmpeg, without normalization, remixing, or replacement. Production SoundPool playback uses `ui_click.wav`; the derivative passed a real-device load test. |

The `ui_click.wav` effect is deliberately absent from the 15-entry `MusicTrack` catalog.

No other music, effects, ambience, voices, jingles, or sound-design scope was introduced.
