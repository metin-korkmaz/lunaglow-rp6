# Privacy

LunaGlow requests Android screen-capture consent only after the user selects
Start ambient capture. Captured frames are downscaled and processed in memory
to derive left and right RGB values.

- Frames are not written to storage.
- Frames and derived colors are not transmitted.
- No accessibility, overlay, location, contacts, microphone, or storage
  permission is requested.
- A persistent notification remains visible while capture is active.
- Protected or DRM content may appear black because Android blocks capture.

The hardware diagnostic reads device build information and LED-class metadata.
It shares that report only after the user explicitly opens Android's Sharesheet.
