#!/usr/bin/env bash

echo "================================================="
echo "🐍 Publishing CogniDB Python Package to PyPI"
echo "================================================="

cd "$(dirname "$0")"

# 1. Clean previous builds
rm -rf build/ dist/ *.egg-info

# 2. Build distributions
python3 setup.py sdist bdist_wheel

# 3. Check twine installation
if ! command -v twine &> /dev/null; then
    echo "📦 Installing twine..."
    pip3 install twine
fi

# 4. Upload to PyPI
echo "🚀 Uploading to PyPI..."
twine upload dist/*

echo "✅ Successfully published cognidb to PyPI!"
echo "Developers can now install via: pip install cognidb"
