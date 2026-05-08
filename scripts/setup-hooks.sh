#!/bin/sh
# Configure git to use project-level hooks (run once per dev machine)
git config core.hooksPath .githooks
chmod +x .githooks/pre-commit
echo "✅ Git hooks activated (.githooks/pre-commit)"
echo "   gitleaks will scan staged files before each commit."
echo ""
echo "   If gitleaks is not installed:"
echo "     winget install gitleaks"
echo "     or: https://github.com/gitleaks/gitleaks/releases"
