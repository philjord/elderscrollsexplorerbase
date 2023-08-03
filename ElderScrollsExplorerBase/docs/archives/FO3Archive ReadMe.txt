------------------------
FO3Archive - Version 1.0
------------------------


Overview
----------------------------------

FO3Archive displays the files in an archive, extracts selected files from the archive, or creates a new archive file.  FO3Archive works with BSA files for Fallout 3.

New archives will be compressed unless the archive contains .mp3 or .ogg files (since these files are already compressed and become larger when compressed a second time).  An archive containing textures may not contain any other files types such as meshes or sounds.

You can extract all of the files or you can select individual files/directories to be extracted.  The extracted files will be placed in the destination directory using the same relative directory structure.

You can create a new archive containing all of the files in subdirectories of the source directory.  Any files in the source directory itself will be ignored.  The relative directory structure will be preserved in the archive.  File names and file paths are limited to a maximum of 254 characters each.


Installation
----------------------------------

To install this utility, place the FO3Archive.jar file into a directory of your choice.  To run the utility, create a program shortcut and specify "javaw -Xmx512m -jar FO3Archive.jar" as the program to run.  Set the start directory to the directory where you extracted the jar file.

A sample program shortcut is included.  The -Xmx512m argument specifies the maximum heap size in megabytes (the example specifies a heap of 512 MB).  You can increase the size if you run out of space processing very large archives.  Note that Windows will start swapping if the Java heap size exceeds the amount of available storage and this will significantly impact performance.

The Sun Java 1.6 runtime is required.  You can download JRE 1.6 from http://java.com/download/index.jsp.  If you are unsure what version of Java is installed on your system, open a command prompt window and enter "java -version".


=========================================================================
=========================================================================


Version 1.0:
------------

Initial release.
