#!/bin/bash

mkdir _crowdin
pushd _crowdin
wget https://crowdin.com/backend/download/project/activitylauncher.zip
unzip activitylauncher.zip
chmod 644 */*.*
chmod 755 *
rm -fr ../descriptions
mv descriptions ..
cp -r res/*  ../ActivityLauncherApp/src/main/res/
popd
rm -fr _crowdin
