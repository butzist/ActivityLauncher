#!/bin/bash

mkdir _crowdin
pushd _crowdin
wget https://crowdin.com/download/project/activitylauncher.zip
unzip activitylauncher.zip
chmod 644 */*.*
chmod 755 *
rm -fr ../descriptions
mv descriptions ..

for d in ?? ??-??; do
   echo $d
   rm -fr ../ActivityLauncherApp/src/main/res/values-$(echo $d | sed 's/-/-r/')
   mv $d ../ActivityLauncherApp/src/main/res/values-$(echo $d | sed 's/-/-r/')
done

popd
pushd ActivityLauncherApp/src/main/res
rm -fr values-sv
mv values-sv-rSE values-sv
rm -fr values-pt
mv values-pt-rPT values-pt
rm -fr values-la
mv values-la-rLA values-la
rm -fr values-zh
mv values-zh-rTW values-zh
rm -fr values-es
mv values-es-rES values-es

popd
rm -fr _crowdin
