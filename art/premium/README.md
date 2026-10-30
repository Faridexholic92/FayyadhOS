# Build-time encoded WebP assets

`*.webp.b64` is standard Base64 of each WebP file, decoded by `generatePremiumAssets` in app/build.gradle. The generated directory is a main Android asset source, including for tests. Only decoded WebP files are packaged.

- world.webp: 1920×1080 clean Kinabalu-inspired dusk/lake background plate; UI and characters are separate native layers.
- birthday.webp: corrected PapaZola/BoBoiBoy fifth-birthday illustration.
- heroes-pose.webp: alpha 626×483 atlas, PapaZola cell (0,0)–(373,483), BoBoiBoy cell (381,0)–(626,367). Default hero composition with native bounded bounce.
- heroes-run.webp: lossless-alpha 1152×640 atlas, 6 columns × 2 rows, cells 192×320. PapaZola row 0; BoBoiBoy row 1 (native rendering mirrors the boy toward PapaZola).

AI-generated concept assets based on the user's approved mockups and supplied character reference; not official franchise assets. See START-HERE-PAPAZOLA.md for attribution and distribution cautions. No downloaded private user-reference file is included.
