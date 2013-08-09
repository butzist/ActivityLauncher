for d in ?? ??-??; do
   mv $d values-$(echo $d | sed 's/-/-r/')
done
mv values-sv-rSE values-sv
mv values-pt-rPT values-pt
#mv values-la-rLA values-la
mv values-zh-rTW values-zh
mv values-es-rES values-es
