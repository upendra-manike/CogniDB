#!/usr/bin/env bash

echo "================================================="
echo "💚 Publishing CogniDB Package to npm Registry"
echo "================================================="

cd "$(dirname "$0")"

# 1. Clean old tarballs
rm -f *.tgz

# 2. Publish to npm
echo "🚀 Publishing to npm..."
npm publish --access public

echo "✅ Successfully published cognidb to npm!"
echo "Developers can now install via: npm install cognidb"
