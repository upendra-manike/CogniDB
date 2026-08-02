#!/usr/bin/env bash
set -e

echo "================================================="
echo "💚 Publishing CogniDB Package to npm Registry"
echo "================================================="

cd "$(dirname "$0")"

# 1. Clean old tarballs
rm -f *.tgz

# 2. Check npm authentication status
if ! npm whoami &> /dev/null; then
    echo "⚠️ You are not logged in to npm!"
    echo "Please run 'npm login' in your terminal first, then re-run this script."
    exit 1
fi

# 3. Publish to npm
echo "🚀 Publishing cognidb-client to npm..."
if npm publish --access public; then
    echo "================================================="
    echo "✅ Successfully published cognidb-client to npm!"
    echo "Developers can now install via: npm install cognidb-client"
    echo "================================================="
else
    echo "================================================="
    echo "❌ Upload failed. Please check your npm permissions."
    echo "================================================="
    exit 1
fi
