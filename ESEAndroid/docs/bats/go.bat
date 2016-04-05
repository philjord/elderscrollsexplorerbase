@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

set destdir=%~1\

echo %destdir%
set /p ok="Is them ok? (Y/N)"
if !ok!==N exit
if !ok!==n exit

cd %destdir%
 
SET "r=%__CD__%"
FOR /R . %%i IN (*.ba2) DO (

  	SET "p=%%i"
 	set "fn=%%~nxi"	

echo ba2extract.exe "!fn!" "!fn!_out"

	ba2extract.exe "!fn!" "!fn!_out"
)
pause