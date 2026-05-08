<p align="center">
  <img src="assets/logo.svg" alt="APA Soft Tools" width="480" />
</p>

# CSP — Cutting Stock Problem Solver for Android

> Native Android application that minimizes raw-material waste when cutting metal bars
> by solving the one-dimensional **Cutting Stock Problem** with optional remnant reuse.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](#)
[![Language](https://img.shields.io/badge/language-Java-007396?logo=java&logoColor=white)](#)
[![Min SDK](https://img.shields.io/badge/minSdk-21-blue)](#)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

---

## Table of contents

- [Overview](#overview)
- [Features](#features)
- [How it works](#how-it-works)
- [Getting started](#getting-started)
- [Usage](#usage)
- [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [License](#license)
- [Author](#author)

---

## Overview

**CSP** is a self-contained Android app for shop-floor and engineering use cases where
linear stock (typically metal bars, tubes, or profiles) must be cut into smaller pieces
with **minimum waste** and the **fewest possible bars consumed**.

The user defines:

- the **number of bars** available,
- the **length of each bar** (in mm),
- a **list of cut types** (length + quantity required),
- an optional **waste threshold** below which a remnant is considered scrap,
- whether **remnant reuse** is enabled.

The app returns a ranked list of solutions with full per-bar cut detail, efficiency
percentage, and one-tap export to JSON or CSV. All scenarios and solutions are persisted
locally with [Room](https://developer.android.com/training/data-storage/room) for later
review.

---

## Features

- 🎯 **Two solver modes**
  - Classic mode (no remnant reuse) — fast, pattern-based.
  - Remnant-reuse mode — Greedy Best-Fit Decreasing + local search for tighter packing.
- 🧮 **Configurable waste threshold** — anything below it is treated as zero waste.
- 📚 **Local history** with swipe-to-delete and one-tap restore.
- 📤 **Export** any solution as JSON or CSV via the standard Android share sheet.
- 🌳 **Cut-tree visualization** showing the exact cut order and remnants per bar.
- 🪶 **Offline-first** — no network calls, no telemetry, no third-party tracking.
- 🍏 **Clean Apple-inspired UI** built on Material Components.

---

## How it works

### Classic solver

Generates feasible cutting **patterns** for a single bar and then assigns multiplicities
to cover the demand while minimizing the number of bars used and the residual waste.
Best suited for instances with a small number of distinct cut lengths.

### Remnant-reuse solver

A two-phase heuristic:

1. **Greedy Best-Fit Decreasing (BFD).** Pieces are sorted by descending length. Each
   piece is placed on the bar whose remaining space exceeds it by the smallest margin.
   A new bar is opened only when no remnant fits.
2. **Local search** (up to 100 iterations). Tries to empty the least-loaded bar by
   moving pieces into other bars' remnants, then performs piece swaps between bars to
   improve the overall fit.

The waste threshold lets the solver treat remnants below a configured size as discardable
(useful when cuts under a certain length are not commercially reusable).

---

## Getting started

### Prerequisites

- **Android Studio** Hedgehog (2023.1) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK** with `compileSdk 34` and `minSdk 21`
- A device or emulator running Android 5.0 (Lollipop) or higher

### Clone and build

```bash
git clone https://github.com/alejandropalaci0/csp-android.git
cd csp-android
./gradlew assembleDebug
```

The debug APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Run on a connected device

```bash
./gradlew installDebug
```

Or open the project in Android Studio and press **Run ▶**.

---

## Usage

1. Launch the app — the splash screen transitions to the main input screen.
2. Enter the **number of bars**, **bar length**, and (optionally) a **waste threshold**.
3. Toggle **Reuse remnants** depending on whether off-cuts can be reused.
4. Tap **+ Add type** to add as many cut types as needed (length + quantity).
5. Tap **🔍 Calculate optimal cuts**.
6. Review the ranked solutions. **Long-press** any solution to save it to history;
   **double-tap** any saved solution to export it as JSON or CSV.
7. Open **📋 History** at any time to revisit, restore, or delete past scenarios.

---

## Project structure

```
app/src/main/
├── java/com/apasoft/csp/
│   ├── data/           # Room DB, DAO, entities
│   ├── domain/         # Solver algorithms
│   ├── model/          # Domain model (CutType, Pattern, Solution, CutTree, CutNode)
│   └── ui/
│       ├── splash/     # SplashActivity
│       ├── main/       # MainActivity + CutTypeAdapter
│       ├── results/    # ResultsActivity + Solution/Remnant adapters
│       └── history/    # HistoryActivity + HistoryAdapter
├── res/
│   ├── layout/         # XML layouts
│   ├── drawable/       # Card and icon drawables
│   └── values/         # colors.xml, strings.xml, themes.xml
└── AndroidManifest.xml
```

---

## Tech stack

| Concern | Tool |
| --- | --- |
| Language | Java 8+ |
| UI | Android Views + Material Components |
| Persistence | Room |
| JSON | Gson |
| Background work | `java.util.concurrent.Executors` |
| Min / Target SDK | 21 / 34 |

---

## License

This project is released under the [MIT License](LICENSE).
© 2026 APASOFT.

---

## Author

Developed by **APASOFT**, part of the **APA Soft Tools** product family.
For questions or commercial inquiries, please open an issue on this repository.