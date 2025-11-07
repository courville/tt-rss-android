#!/bin/bash

case `uname` in
  Linux)
    export CORES=$((`nproc`+1))
  ;;
  Darwin)
    # assumes brew install coreutils in order to support readlink -f on macOS
    export CORES=$((`sysctl -n hw.logicalcpu`+1))
  ;;
esac

cd ..
prefix=`pwd`
bdir=$prefix/org.fox.ttrss/build/outputs/apk/release

echo "Building release..."
./gradlew assembleRelease --parallel --max-workers=${CORES}
if [ $? -ne 0 ]; then
  echo "Build failed."
  exit 1
fi

apk_file=$(find $bdir -name "ttrss-*.apk" | head -n 1)

if [ -z "$apk_file" ]; then
  echo "No APK file found in $bdir"
  exit 1
fi

version=$(basename "$apk_file" | sed -e 's/ttrss-//' -e 's/\.apk//')

cd $prefix/release

last_tag=$(git describe --tags --abbrev=0)
echo "Generating release notes from commits since ${last_tag}"
git log ${last_tag}..HEAD --pretty=format:"- %s" > release-notes.tmp

# Filter out merge commits
grep -v "Merge " release-notes.tmp > release-notes.tmp2

# Handle Weblate commits
if grep -q "Weblate" release-notes.tmp2; then
    grep -v "Weblate" release-notes.tmp2 > release-notes.txt
    echo "- Weblate translations update" >> release-notes.txt
else
    mv release-notes.tmp2 release-notes.txt
fi

rm release-notes.tmp release-notes.tmp2

echo "Uploading release v${version} to github..."

ls $bdir
echo "uploading"
./push.py "v${version}" "v${version} release" "$bdir"

if [ $? -eq 0 ]; then
    echo "Release successful. Tagging release with v${version}"
    git tag "v${version}"
    echo "Run 'git push --tags' to push the tag to the remote."
fi
