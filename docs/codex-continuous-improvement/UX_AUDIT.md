# UX Audit — Cycle 001 Baseline

## Product direction
The intended experience is a unified, offline-first document workspace. Current navigation exposes Home, Scanner, Vault, Convert, PDF tools, and History, with dedicated PDF/Image hubs added recently.

## Verified UX risks
1. **Shared-file continuity:** `DocForgeNavHost` stores `activeSharedLaunch` with plain `remember`. Activity/configuration recreation can discard the in-progress shared launch before a destination consumes it. This is a high-priority continuity defect.
2. **Settings reads in navigation:** several ViewModel factories call `settingsRepository.currentSettings()` from composable destination construction. Even small synchronous preference reads belong outside composition/navigation creation and can contribute to cold-path jank.
3. **Bottom-navigation density:** current scaffold exposes six bottom destinations (Home, Scanner, Vault, Convert, PDF tools, History). This needs small-screen, one-handed, label-length, and accessibility validation rather than assuming more tabs are better.
4. **Hard-coded strings:** verified examples such as “Vault” remain hard-coded in navigation while many other labels use string resources. Full localization audit is required.
5. **Compression semantics:** PDF compression rasterizes pages. The UI must clearly communicate that selectable/vector text can be lost and quality can change.
6. **OCR internationalization:** searchable-PDF overlay currently uses Helvetica/Type1 assumptions. Non-Latin OCR text needs a robust offline font strategy before global-quality claims.

## Required UX validation
- 320–360dp width and large font-scale layouts.
- TalkBack semantics, focus order, content descriptions, and 48dp touch targets.
- RTL navigation and tool screens.
- Long translated labels.
- Empty/loading/error/success/cancellation states for every long operation.
- Back behavior during processing.
- Shared-intent recovery after rotation/theme/configuration changes.
- Output collision messaging should not ask users to overwrite; generated suffixes should be discoverable in success UI/history.

## Current conclusion
The app has a strong breadth of workflows, but Cycle 001 should prioritize continuity, data safety, predictable output handling, and accessibility consistency before adding more visible tools.
