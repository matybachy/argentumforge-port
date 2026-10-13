# ArgentumForge

Experimental Forge 1.8.9 port of Argentum, built as a separate mod project in this repository.

Current scope
- Forge 1.8.9 / Java 8 / LWJGL 2 target
- Independent mod ID: `argentumforge`
- Client render hook active: `RenderWorldLastEvent`
- Chunk-render prototype in progress: mesh generation and VBO upload path added
- Vanilla fallback preserved; experimental renderer is toggled by code and can be disabled

Repository status
- This repo is a working prototype, not the complete upstream Argentum feature set.
- The project is intentionally kept private and separate from upstream repositories.
- Upstream Argentum and Celeritas are used as read-only reference sources only.

Build
- Java 8 is required.
- Run from the repo root:
  - ./gradlew clean build --stacktrace --no-daemon
  - or on Windows: .\gradlew.bat clean build --stacktrace --no-daemon

Known limits
- This is not a complete port of the upstream Argentum renderer.
- The current terrain prototype is a proof of concept for the Forge 1.8.9 rendering pipeline.
- Real block-face meshing, lighting, AO, texture mapping, and per-layer renders remain under active development.
- The experimental renderer is intended to be disabled by default unless specifically enabled for a runtime test.

This repository is intended as a private development workspace for a Forge 1.8.9 rendering port, not a public distribution.
