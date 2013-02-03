#!/bin/bash

apipath="androidapis"
inputpath="origapks"
outputpath="ponyapks"

libraries=`
	for lib in \`ls .modder/libs\`; do
		echo -n ":.modder/libs/$lib";
	done;`

classpath=".modder/bin"
mainclass="net.fimfiction.tgtipmeogc.main.Example"

mods=".mods/bin/Mods Container.apk"

java -cp "$classpath$libraries" $mainclass "$inputpath/$1" "$mods" "$outputpath/$1" "$apipath/8.xml"


