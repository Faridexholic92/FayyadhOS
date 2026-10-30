# Build-time encoded WebP assets

`*.webp.b64` is standard Base64 of each WebP file, decoded by `generatePremiumAssets` in app/build.gradle. The generated directory is a main Android asset source, including for tests. Only decoded WebP files are packaged.

- world.webp: 1920×1080 clean Kinabalu-inspired dusk/lake background plate; UI and characters are separate native layers.
- birthday.webp: corrected PapaZola/BoBoiBoy fifth-birthday illustration.
- fayyadhos-logo.webp: historical screenshot-derived logo only; excluded by generatePremiumAssets in 3.3. Header now uses FayyadhWordmarkView native vector lettering, with no image crop/keying.
- heroes-blended.webp: 755×600 current display atlas. PapaZola remains at (0,0)–(373,483); the new 3D running Halilintar is at (381,0)–(755,600), generated as a costume/pose-style edit from the supplied cheerful running mockup and Halilintar outfit references. Chroma matte removed, edges despilled, then dusk/ground graded. This is an AI fan-concept rendition, not the original flat crouching upload or an official MONSTA asset.
- heroes-pose.webp: lossless-alpha 631×483 atlas: PapaZola cell (0,0)–(373,483); supplied BoBoiBoy Halilintar cell (381,0)–(631,250). User upload Boboiboy_Halilintar.webp is preserved pixel-for-pixel as decoded RGBA. Historical source atlas retained for provenance; the current default/optional free-run display uses the 3D character in heroes-blended.webp.
- heroes-run.webp: lossless-alpha 1152×640 atlas, 6 columns × 2 rows, cells 192×320. PapaZola row 0 is used for optional free-run; legacy BoBoiBoy row 1 is no longer rendered.

PapaZola/background and the current 3D running Halilintar are AI-generated fan-concept assets. The costume edit is guided by user references; the unedited flat Halilintar upload is retained only in the historical source atlas. The wordmark is native vector lettering. No official franchise-asset licence is implied. See START-HERE-PAPAZOLA.md for attribution and distribution cautions. No downloaded private user-reference file is included.
