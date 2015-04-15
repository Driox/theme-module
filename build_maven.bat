@echo off
echo start release

echo build the module
call play build-module

echo build maven artifact
call mvn install:install-file -DgroupId=ThemeModule -DartifactId=ThemeModule -Dversion=1.4 -Dfile=./lib/play-ThemeModule.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=./repository  -DcreateChecksum=true
