@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

set srcdir=%~1\
set destdir=%~dp1%~nx1_tga_ktx\

echo %srcdir%
echo %destdir%
set /p ok="Is them ok? (Y/N)"
if !ok!==N exit
if !ok!==n exit

set /p rec="Make the conversion recursively? (Y/N)"
set dorec=false
if !rec!==y set dorec=true
if !rec!==Y set dorec=true

rem copy the non dds files with the folder structure
if !dorec!==true (
	rem exclude the dds but copy all others
	xcopy "%srcdir%*.*" "%destdir%" /c /s /r /d /y /i /q /exclude:D:\game_media\bats\excludedds.txt
)

rem copy the dds files only one folder deep
if !dorec!==false (
	xcopy "%srcdir%*.dds" "%destdir%" /c /r /d /y /i /q /exclude:D:\game_media\bats\excludedds.txt
)

echo [!TIME!] Copy finished

pause
exit