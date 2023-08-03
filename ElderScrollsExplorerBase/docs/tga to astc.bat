@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

set srcdir=%~1\
set destdir=%~dp1%~nx1_astc\

echo %srcdir%
echo %destdir%
set /p ok="Is them ok? (Y/N)"
if !ok!==N exit
if !ok!==n exit

set /p del="Delete copied tga files after convert? (Y/N)"
set dodel=false
if !del!==y set dodel=true
if !del!==Y set dodel=true

set /p rec="Make the conversion recursively? (Y/N)"
set dorec=false
if !rec!==y set dorec=true
if !rec!==Y set dorec=true

set /p bpp="Enter a bpp in decimal (e.g. 8.0) or XxY"

rem copy the tga files with the folder structure
if !dorec!==true xcopy "%srcdir%*.tga" "%destdir%" /c /s /r /d /y /i /q

rem copy the tga files only one folder deep
if !dorec!==false xcopy "%srcdir%*.tga" "%destdir%" /c /r /d /y /i /q

rem flush log
rem break > %~dp0\convert2_log.txt

rem convert the tga files to astc
for /r %destdir% %%i in (*.tga) do ( 
	set file=%%~ni
	echo [!TIME!] Converting: !file!
	%~dp0\astcenc.exe -cl "%%i" "%%i".astc %bpp% -medium
	rem echo [!TIME!] Converted: %%i >> %~dp0\convert2_log.txt
)

rem delete copied tga files
if !dodel!==true (
	del /S "%destdir%*.tga" 
)

rem echo [!TIME!] Convert process READY >> %~dp0\convert2_log.txt
echo [!TIME!] Convert process READY

pause
exit