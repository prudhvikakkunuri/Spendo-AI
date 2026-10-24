# Contributing to Spendo AI

Thanks for helping improve Spendo AI.

## Before opening a pull request

1. Open an issue to discuss a significant feature or behavioural change.
2. Keep the change focused and compatible with the current Kotlin/Android architecture.
3. Add or update tests for changed behaviour.
4. Run `./gradlew test` and `./gradlew assembleDebug` locally when possible.
5. Update the README if setup, behaviour, permissions, or supported formats change.

## Privacy and financial data

- Use synthetic transactions and fake account identifiers in tests.
- Never commit API keys, passwords, keystores, bank statements, real SMS/notification text, or personal financial information.
- Do not weaken permission checks, account ownership checks, backup exclusions, or transaction integrity to make a test pass.
- Keep ambiguous transaction parsing safe and reviewable rather than guessing.

## Pull requests

Describe the problem, implementation, tests run, and any known limitations. Include screenshots for user-visible changes when useful, and remove any private information from screenshots.
