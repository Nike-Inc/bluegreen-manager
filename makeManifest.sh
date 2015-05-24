#!/bin/bash
# makeManifest.sh
#
# After updating pom.xml deps, run 'mvn clean package dependency:copy-dependencies'
# and then this script to update MANIFEST.MF.  Then run the mvn command again
# to put the updated manifest in bluegreen-manager.jar.
# And remember to checkin the updated MANIFEST.MF.

\ls target/dependency | awk 'BEGIN{printf("Manifest-Version: 1.0\nClass-Path:")} {printf("  dependency/%s\n", $0);} END{printf("Main-Class: bluegreen.manager.main.BlueGreenManager\n")}' > src/main/resources/META-INF/MANIFEST.MF

