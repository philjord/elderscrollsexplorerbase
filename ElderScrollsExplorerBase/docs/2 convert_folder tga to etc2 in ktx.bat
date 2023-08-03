@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

set srcdir=%~1\
set destdir=%~dp1%~nx1_ktx\

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

rem copy the tga files with the folder structure
if !dorec!==true xcopy "%srcdir%*.tga" "%destdir%" /c /s /r /d /y /i /q

rem copy the tga files only one folder deep
if !dorec!==false xcopy "%srcdir%*.tga" "%destdir%" /c /r /d /y /i /q

copy %~dp0\convert.exe "%destdir%" /Y
copy %~dp0\etcpack.exe "%destdir%" /Y
copy %~dp0\identify.exe "%destdir%" /Y
 
rem break > %~dp0\convert4_log.txt

cd %destdir%
 
SET "r=%__CD__%"
FOR /R . %%i IN (*.tga) DO (

  	SET "p=%%i"
 	set "fn=%%~nxi"	
	set "pnr=!p:%r%=!" 
  	
	call set subpath=%%pnr:!fn!=%%

	set file=%%~ni
	echo [!TIME!] Converting: !file!

	if "!subpath!"=="" ( set subpath="." ) else (call set subpath=%%subpath:~,-1%% )


	rem I should really run once and parse theoutput, cos I want sRGB color space too
	set hasAlpha=false
	set has1BitAlpha=false



	rem newer way with less calls to idetify, but more disk access so unsure

	identify.exe -verbose "%%i" > temp.txt
	FIND temp.txt /I "alpha: 8-bit">Nul && ( set hasAlpha=true ) 
	FIND temp.txt /I "alpha: 4-bit">Nul && ( set hasAlpha=true )
	FIND temp.txt /I "alpha: 1-bit">Nul && ( set has1BitAlpha=true ) 


	rem identify.exe -verbose "%%i" | FIND /I "alpha: 8-bit">Nul && ( set hasAlpha=true ) 
	rem identify.exe -verbose "%%i" | FIND /I "alpha: 4-bit">Nul && ( set hasAlpha=true )
	rem identify.exe -verbose "%%i" | FIND /I "alpha: 1-bit">Nul && ( set has1BitAlpha=true )

	if !hasAlpha!==false (
		if !has1BitAlpha!==false (
			echo RGB
			etcpack.exe "%%i" !subpath! -f RGB -mipmaps -ktx  
		) ELSE (
			echo RGBA1
			etcpack.exe "%%i" !subpath! -f RGBA1 -mipmaps -ktx  
		)
	) ELSE (		
		echo RGBA
		etcpack.exe "%%i" !subpath! -f RGBA -mipmaps -ktx  			
	)
	
	rem echo [!TIME!] Converted: %%i >> %~dp0\convert4_log.txt 

)

rem delete copied tga files
if !dodel!==true (
	del /S "%destdir%*.tga" 
)

del "%destdir%\convert.exe"
del "%destdir%\etcpack.exe"
del "%destdir%\identify.exe"

del  "%destdir%\temp.txt"

echo [!TIME!] Convert process READY

pause
 