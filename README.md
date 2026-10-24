# Spendo AI

<p align="center">
  <strong>Personal finance, made clearer.</strong><br/>
  An AI-powered mobile experience for tracking expenses, understanding spending, and building better money habits.
</p>

<p align="center">
  <img alt="Project status" src="https://img.shields.io/badge/status-in%20development-orange" />
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android%20focused-3DDC84?logo=android&logoColor=white" />
  <img alt="Repository visibility" src="https://img.shields.io/badge/repository-public-blue" />
</p>

---

## Overview

**Spendo AI** is a personal finance application in development that aims to bring everyday money management and AI assistance into one focused mobile experience. The project is being built around a simple goal: help people understand where their money goes without making money management feel complicated.

This repository is the central home for the application's source code, technical documentation, and project development.

> **Project status:** In active development. The repository is being prepared for the initial application code upload. Features, architecture, and setup instructions will be updated as implementation progresses.

## Product direction

The following describes the current product direction. These capabilities should not be considered shipped until implemented and documented.

- **Expense and income tracking** — record transactions and keep financial activity organized.
- **Categories and budgets** — group transactions and compare spending against planned budgets.
- **Spending insights** — make personal spending patterns easier to review.
- **AI assistant** — provide a conversational interface for finance-related questions, with AI activation configured by the user.
- **User-configured AI provider** — allow users to enter an API key before enabling the chatbot, subject to the provider and integration ultimately supported.
- **Thoughtful mobile experience** — prioritize a clean interface, responsive interactions, and efficient everyday workflows.
- **Privacy-conscious design** — handle financial data and credentials with care.

## Architecture and technology

The final stack will be confirmed after the initial source code is available. The current direction includes:

| Area | Current direction |
| --- | --- |
| Client | Android-focused mobile application |
| Data and backend | Supabase |
| AI | User-configured provider integration |
| Source control | Git and GitHub |

Specific frameworks, supported AI providers, data models, and deployment details will be documented after they are confirmed in the codebase. Avoid relying on this table as a final architecture specification.

## Getting started

### Clone the repository

```bash
git clone https://github.com/prudhvikakkunuri/Spendo-AI.git
cd Spendo-AI
```

### Development setup

The application source and dependency manifests have not yet been documented here. Setup instructions will be added once the initial code is pushed, including the required toolchain, configuration, and run/build commands.

## Configuration and secrets

Before running the application, configure any required service credentials using the documented local development process.

- Never commit API keys, access tokens, passwords, private signing keys, or database credentials.
- Keep local secret/configuration files out of version control and provide a sanitized `.env.example` or equivalent template when needed.
- Do not include Supabase service-role keys in a mobile client. Use appropriate server-side boundaries for privileged operations.
- Configure Supabase Row Level Security (RLS) and least-privilege access policies before using real personal financial data.
- Treat user-provided AI credentials as sensitive. Use platform-appropriate secure storage and avoid logging keys or sending them to unrelated services.
- If a credential is exposed, revoke or rotate it immediately. Removing it in a later commit does not make the exposed credential safe.

## Repository conventions

To keep the project maintainable as it grows:

- Prefer focused, descriptive commits.
- Keep documentation updated alongside changes to setup, features, or architecture.
- Do not commit generated build output, local IDE state, personal data, or secrets.
- Use issues to track bugs and scoped improvements.
- Review changes before merging them into the main development branch.

## Roadmap

This roadmap is directional and will evolve with implementation.

- [ ] Add the initial application source code and dependency manifests.
- [ ] Document the development environment and local run instructions.
- [ ] Implement transaction entry and expense/income history.
- [ ] Add categories, budgets, and spending summaries.
- [ ] Integrate the AI assistant and its user-controlled configuration flow.
- [ ] Define secure persistence and Supabase access policies.
- [ ] Add automated checks and core tests.
- [ ] Document release, troubleshooting, and contribution workflows.

## Issues and feedback

Please use [GitHub Issues](https://github.com/prudhvikakkunuri/Spendo-AI/issues) to report a reproducible bug or propose an improvement. Include relevant context, expected behavior, actual behavior, and steps to reproduce where applicable. Do not include credentials, account details, or real financial records in issue reports.

## License

No license has been declared yet. Unless and until a license is added, no permission is granted to reuse, modify, or redistribute this project's code beyond rights provided by applicable law.

---

<p align="center">
  <sub>Spendo AI · A personal finance project in development</sub>
</p>
