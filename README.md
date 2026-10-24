# Spendo AI 💸

**Spendo AI** is an AI-powered personal finance and expense management app designed to help users track spending, understand their habits, and manage everyday money with a modern mobile experience.

> 🚧 **Status: In development.** This repository is being prepared for the initial application code upload. Features and setup steps will be updated as the app evolves.

## ✨ Project Vision

Spendo AI aims to bring everyday expense management and AI assistance together in one simple app.

- **Expense tracking** — record and organize everyday spending.
- **Spending insights** — make spending patterns easier to understand.
- **Budget management** — plan budgets and monitor expenses against them.
- **AI assistant** — configure an AI provider with your own API key before activating the chatbot.
- **Modern mobile UI** — focus on a clean, minimal interface and smooth interactions.
- **Privacy-minded design** — handle credentials carefully and avoid exposing secrets in source control.

The features above describe the project direction; not all features are implemented yet.

## 🧰 Planned Technology

The implementation details will be confirmed once the application code is uploaded. The current direction includes:

- **Platform:** Android-focused mobile app
- **Backend and data:** Supabase
- **AI integration:** User-configured AI provider and API key

The final architecture and supported AI providers will be documented as they are implemented.

## 🚀 Getting Started

Setup instructions will be added after the source code and dependency files are pushed.

To get the repository locally:

```bash
git clone https://github.com/prudhvikakkunuri/Spendo-AI.git
cd Spendo-AI
```

Once the application files are available, follow the platform-specific installation instructions added to this README.

## 🔐 Security & Privacy

- **Never commit API keys, access tokens, passwords, signing keys, or service-role keys.**
- Keep local secrets in appropriate environment or platform-specific secure configuration and exclude secret files from Git.
- Do not expose privileged Supabase keys in a client-side mobile app.
- Configure appropriate Supabase access controls and Row Level Security (RLS) policies before storing personal financial data.
- Request only the permissions necessary for the app's features.
- Treat AI-generated financial information as general assistance, not professional financial advice.

If a secret is accidentally committed, revoke or rotate it; deleting it from a later commit does not make the exposed secret safe.

## 🗺️ Roadmap

- [ ] Push the initial application source code
- [ ] Document development prerequisites and installation
- [ ] Build expense and income tracking
- [ ] Add categories, budgets, and spending summaries
- [ ] Integrate the AI chatbot with user-configured credentials
- [ ] Configure secure Supabase access and data policies
- [ ] Add testing, troubleshooting, and release instructions

Roadmap items are planned goals, not claims of completed functionality.

## 🐛 Issues & Feedback

Found a bug or have an idea? Open a [GitHub Issue](https://github.com/prudhvikakkunuri/Spendo-AI/issues) with the steps to reproduce the problem or a clear description of the proposed improvement.

## 📄 License

A license has not been selected yet. Until one is added, do not assume the source code is available for reuse, modification, or redistribution.

---

Made with 💚 for simpler personal finance.
