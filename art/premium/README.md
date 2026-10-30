# Build-time encoded WebP assets

`*.webp.b64` is standard Base64 of each WebP file, decoded by `generatePremiumAssets` in app/build.gradle. The generated directory is a main Android asset source, including for tests. Only decoded WebP files are packaged.

- world.webp: 1920×1080 clean Kinabalu-inspired dusk/lake background plate; UI and characters are separate native layers.
- birthday.webp: corrected PapaZola/BoBoiBoy fifth-birthday illustration.
- fayyadhos-logo.webp: transparent cutout of the user's supplied 2172×724 reference wordmark, downsampled to 1024 pixels wide after removing the baked-in checkerboard. Not AI-redrawn.
- heroes-blended.webp: 631×483 display atlas derived from heroes-pose.webp, with blue-hour ambient colour, warm screen-right edge light and restrained ground-contact grading. Source identity and silhouette retained; display RGB is deliberately colour-adjusted, not pixel-identical to the upload.
- heroes-pose.webp: lossless-alpha 631×483 atlas: PapaZola cell (0,0)–(373,483); supplied BoBoiBoy Halilintar cell (381,0)–(631,250). User upload Boboiboy_Halilintar.webp is preserved pixel-for-pixel as decoded RGBA. Default composition is static; optional free-run also uses this Halilintar cell, not the old boy sprite row.
- heroes-run.webp: lossless-alpha 1152×640 atlas, 6 columns × 2 rows, cells 192×320. PapaZola row 0 is used for optional free-run; legacy BoBoiBoy row 1 is no longer rendered.

PapaZola and background are AI-generated fan-concept assets; Halilintar is the user-supplied transparent image, not AI-generated. No official franchise-asset licence is implied. See START-HERE-PAPAZOLA.md for attribution and distribution cautions. No downloaded private user-reference file is included.
