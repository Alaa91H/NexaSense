# Google Sans

The app's UI typeface: the official **Google Sans** variable font, the same
family Google uses across its 2026 products (Android, Clock, Search, etc.).

## Source

- Family: Google Sans (variable, axes `wght` 400–700, `opsz` 17–18, `GRAD`)
- Distribution: [Google Fonts](https://fonts.google.com/specimen/Google+Sans)
- Downloaded: 2026-08-13 from the official Google Fonts CDN
  (`fonts.gstatic.com/s/googlesans/...`), served as an OFL-licensed font.
- File: `presentation/src/main/res/font/google_sans.ttf` (1.9 MB variable TTF)

## License

SIL Open Font License 1.1 — see `OFL.txt` in this directory.
Reserved Font Name: Google Sans.

## Notes

- Google Sans does **not** include Arabic glyphs. Arabic UI text falls back to
  the platform default (Noto Sans Arabic on Android), exactly as Google's own
  Arabic products behave — Latin text and the large compass/level numerals
  render in Google Sans.
- The variable font is instantiated per weight by Compose (`Font(R.font.google_sans,
  weight = ...)`); `minSdk` 31 guarantees full variable-font support.
