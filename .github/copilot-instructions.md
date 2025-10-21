
# Copilot instructions for this repository

- Before making changes, read README.md and other top-level docs for project intent, configuration, and usage details. Use README context to guide naming, behavior, and UI text.
- Target compatibility with the latest RuneLite release and its current APIs. Prefer documented public APIs; avoid deprecated or internal/private APIs.
- Verify API usage against the official RuneLite docs and codebase (https://github.com/runelite/runelite and RuneLite javadoc) before implementing.
- Follow the repository's existing coding style, formatting, package structure, and dependency management (Gradle/Maven). Match annotation, logging, and dependency versions already in the project.
- Implement features using the RuneLite plugin framework: use Plugin/PluginDescriptor, subscribe to events via @Subscribe, use ClientThread when required, and respect thread-safety rules.
- Keep plugin manifest, resource paths, and configuration keys consistent with README and existing files. Update README or changelog if behavior, commands, or config defaults change.
- Write concise, well-documented code and include unit tests / integration checks where appropriate. Add or update plugin tests to cover new behavior.
- Ensure code handles nulls and edge cases and preserves RuneLite performance characteristics (avoid heavy work on the client thread).
- When adding new external dependencies, prefer small, well-maintained libraries and document why they're needed in README or build files.
- If unsure about an API or design choice, add a TODO with rationale and include a reference to the RuneLite documentation or issue for follow-up.
- Keep commits focused and small; include descriptive commit messages and update any version or manifest files as required.
- Pick only one feature at a time as described in the readme.

DO NOT:
- Use hard-coded RuneLite internal paths or rely on unpublished internal APIs.
- Ignore the repository README or existing plugin conventions.

When you are finished reanalyze your own work and compare it to the instructions given and reply with a ✅ compliant if all checks out or a ❌ for a mismatch issue with implementation and what was described be the user and repository notes.