# Contributing to QueryMe

Thank you for your interest in contributing to QueryMe!
This project is developed collaboratively, and we welcome all contributions.

---

## Workflow Overview

* `main` → stable production-ready code
* `develop` → active development branch

**All pull requests MUST target the `develop` branch (NOT `main`)**

---

## 🚀 Getting Started

### 1. Fork the Repository

Click the **Fork** button on GitHub.

### 2. Clone Your Fork

```bash
git clone https://github.com/your-username/your-repo.git
cd your-repo
```

### 3. Add Upstream Remote

```bash
git remote add upstream https://github.com/original-owner/your-repo.git
```

### 4. Create a Feature Branch (from `develop`)

```bash
git checkout develop
git pull upstream develop
git checkout -b feature/your-feature-name
```

---

## 🛠️ Development Guidelines

* Keep your code clean and readable
* Follow project structure and conventions
* Write meaningful variable and function names
* Add comments where necessary

---

## Commit Guidelines

Use clear and consistent commit messages:

* `feat: add new feature`
* `fix: resolve bug`
* `docs: update documentation`
* `refactor: improve code structure`

---

## Keeping Your Branch Updated

Before pushing changes:

```bash
git checkout develop
git pull upstream develop
git checkout feature/your-feature-name
git merge develop
```

---

## Submitting a Pull Request

1. Push your branch:

```bash
git push origin feature/your-feature-name
```

2. Open a Pull Request

3. IMPORTANT:

   * Base branch must be: `develop`
   * NOT `main`

4. In your PR description:

   * Describe your changes
   * Reference related issues (if any)

---

## Testing

* Ensure your changes do not break existing functionality
* Test your feature before submitting a PR

---

## Reporting Issues

If you find a bug:

* Check existing issues first
* If not found, create a new issue with:

  * Clear description
  * Steps to reproduce
  * Expected behavior

---

## Feature Requests

Have an idea?
Open an issue and describe:

* What you want to add
* Why it’s useful

---

## Code of Conduct

Be respectful and constructive in all interactions.

---

## Thank You

Your contributions help make QueryMe better!
