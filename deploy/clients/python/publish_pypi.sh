#!/usr/bin/env bash
set -e

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
if twine upload dist/*; then
    echo "================================================="
    echo "✅ Successfully published cognidb-client to PyPI!"
    echo "Developers can now install via: pip install cognidb-client"
    echo "================================================="
else
    echo "================================================="
    echo "❌ Upload failed. Please verify your PyPI API token."
    echo "Note: Username must be '__token__' and Password must be 'pypi-...' API token from https://pypi.org/manage/account/token/"
    echo "================================================="
    exit 1
fi
